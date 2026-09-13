package com.workflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workflow.approval.common.ResultCode;
import com.workflow.approval.dto.BranchSaveRequest;
import com.workflow.approval.dto.BranchVO;
import com.workflow.approval.dto.DraftSaveRequest;
import com.workflow.approval.dto.ProcessDraftVO;
import com.workflow.approval.dto.ProcessNodeSaveRequest;
import com.workflow.approval.dto.ProcessNodeVO;
import com.workflow.approval.entity.ProcessDefinition;
import com.workflow.approval.entity.ProcessNode;
import com.workflow.approval.entity.SysUser;
import com.workflow.approval.exception.BusinessException;
import com.workflow.approval.mapper.ProcessDefinitionMapper;
import com.workflow.approval.mapper.ProcessNodeMapper;
import com.workflow.approval.mapper.SysUserMapper;
import com.workflow.approval.security.UserContext;
import com.workflow.approval.service.ProcessDesignerService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程设计器草稿服务。
 * <p>
 * 拓扑以递归树表达：主链为节点数组，网关节点携带多条分支，分支内 nodes 仍是节点数组（可继续嵌套网关）。
 * 持久化为 {@code wf_process_node} 扁平邻接表：
 * <ul>
 *   <li>审批行 APPROVAL、分叉行 GATEWAY_FORK（joinNodeId 指向汇聚行）、汇聚行 GATEWAY_JOIN：branchGroup 为空；</li>
 *   <li>分支锚点行 GATEWAY_BRANCH 与分支内子节点：parentId=分叉行ID，branchGroup=组标识（g{forkId}_{序号}）；</li>
 *   <li>主链节点 parentId=0。</li>
 * </ul>
 * 保存采用事务内全量重建，规避复杂嵌套结构的增量对齐。
 */
@Service
@RequiredArgsConstructor
public class ProcessDesignerServiceImpl implements ProcessDesignerService {

    /** 主干审批链的固定标识，当前系统仅此一条流程定义 */
    private static final String DEFAULT_PROCESS_KEY = "approval_main";
    private static final String DEFAULT_PROCESS_NAME = "主干审批流程";
    private static final String STATUS_DRAFT = "DRAFT";

    private static final long ROOT_PARENT_ID = 0L;
    /** 汇聚行在分叉作用域内的固定排序（每个分叉仅一条汇聚行） */
    private static final int JOIN_SORT = 0;
    /** 分支锚点行排序基数：与分支内节点（从0起）同组，锚点用大基数避免混淆 */
    private static final int BRANCH_ANCHOR_SORT_BASE = 100000;

    private static final int MAX_NODE_COUNT = 200;
    private static final int MAX_DEPTH = 10;
    private static final int MIN_BRANCHES = 2;

    private static final String TYPE_APPROVAL = "APPROVAL";
    private static final String TYPE_GATEWAY = "GATEWAY";
    private static final String ROW_FORK = "GATEWAY_FORK";
    private static final String ROW_BRANCH = "GATEWAY_BRANCH";
    private static final String ROW_JOIN = "GATEWAY_JOIN";
    private static final String MODE_PARALLEL = "PARALLEL";
    private static final String MODE_EXCLUSIVE = "EXCLUSIVE";

    private static final Set<String> SUPPORTED_APPROVER_TYPES = Set.of("USER", "DEPT_LEADER", "ROLE_ADMIN");
    private static final Set<String> SUPPORTED_GATEWAY_MODES = Set.of(MODE_PARALLEL, MODE_EXCLUSIVE);

    private final ProcessDefinitionMapper processMapper;
    private final ProcessNodeMapper nodeMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessDraftVO getOrCreateDraft() {
        return toDraftVO(getOrCreateDefaultProcess());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessDraftVO saveDraft(DraftSaveRequest request) {
        ProcessDefinition process = getOrCreateDefaultProcess();

        process.setProcessName(request.getProcessName().trim());
        process.setRemark(request.getRemark());
        process.setStatus(STATUS_DRAFT);
        process.setUpdateBy(UserContext.getCurrentUserId());
        processMapper.updateById(process);

        List<ProcessNodeSaveRequest> rootChain = request.getNodes() == null
                ? new ArrayList<>() : request.getNodes();

        // 全量重建：先级联清空旧拓扑（扁平表，按 processId 一次删除即可）
        nodeMapper.delete(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getProcessId, process.getId()));

        validateAndPersistChain(process.getId(), rootChain, ROOT_PARENT_ID, null);

        return toDraftVO(process);
    }

    // ---------------- 校验 + 持久化（树 → 行） ----------------

    private void validateAndPersistChain(Long processId, List<ProcessNodeSaveRequest> chain,
                                         Long parentId, String branchGroup) {
        Set<Long> approverUserIds = new HashSet<>();
        int[] counter = new int[1];
        collectValidation(chain, 1, approverUserIds, counter);

        Set<Long> validUserIds = approverUserIds.isEmpty() ? new HashSet<>()
                : new HashSet<>(userMapper.selectBatchIds(approverUserIds).stream().map(SysUser::getId).toList());

        persistChain(processId, chain, parentId, branchGroup, 0, validUserIds);
    }

    /** 递归校验整棵树：类型、名称、网关分支、嵌套深度、节点总量；收集 USER 类审批人ID */
    private void collectValidation(List<ProcessNodeSaveRequest> chain, int depth,
                                   Set<Long> approverUserIds, int[] counter) {
        if (depth > MAX_DEPTH) {
            throw new BusinessException(ResultCode.PROCESS_NODE_DEPTH_TOO_DEEP);
        }
        for (ProcessNodeSaveRequest node : nullSafe(chain)) {
            counter[0]++;
            if (counter[0] > MAX_NODE_COUNT) {
                throw new BusinessException(ResultCode.PROCESS_NODE_TOO_MANY);
            }
            if (node == null) {
                throw new BusinessException(ResultCode.PROCESS_TOPOLOGY_INVALID);
            }

            String type = node.getType();
            if (TYPE_GATEWAY.equals(type)) {
                String mode = node.getGatewayMode();
                if (!StringUtils.hasText(mode) || !SUPPORTED_GATEWAY_MODES.contains(mode.trim())) {
                    throw new BusinessException(ResultCode.PROCESS_GATEWAY_MODE_INVALID);
                }
                List<BranchSaveRequest> branches = node.getBranches();
                if (branches == null || branches.size() < MIN_BRANCHES) {
                    throw new BusinessException(ResultCode.PROCESS_GATEWAY_BRANCHES_INVALID);
                }
                for (int i = 0; i < branches.size(); i++) {
                    BranchSaveRequest branch = branches.get(i);
                    if (branch == null) {
                        throw new BusinessException(ResultCode.PROCESS_TOPOLOGY_INVALID);
                    }
                    if (MODE_EXCLUSIVE.equals(mode.trim())
                            && !StringUtils.hasText(branch.getConditionName())) {
                        throw new BusinessException(ResultCode.PROCESS_BRANCH_CONDITION_REQUIRED);
                    }
                    if (StringUtils.hasText(branch.getConditionExpr())
                            && branch.getConditionExpr().length() > 255) {
                        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "条件表达式最长 255 个字符");
                    }
                    collectValidation(branch.getNodes(), depth + 1, approverUserIds, counter);
                }
            } else if (TYPE_APPROVAL.equals(type)) {
                validateApproval(node, approverUserIds);
            } else {
                throw new BusinessException(ResultCode.PROCESS_TOPOLOGY_INVALID);
            }
        }
    }

    private void validateApproval(ProcessNodeSaveRequest node, Set<Long> approverUserIds) {
        if (!StringUtils.hasText(node.getNodeName())) {
            throw new BusinessException(ResultCode.PROCESS_NODE_NAME_BLANK);
        }
        if (node.getNodeName().trim().length() > 64) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "节点名称最长 64 个字符");
        }
        String approverType = node.getApproverType();
        if (!StringUtils.hasText(approverType) || !SUPPORTED_APPROVER_TYPES.contains(approverType.trim())) {
            throw new BusinessException(ResultCode.PROCESS_APPROVER_TYPE_INVALID);
        }
        if ("USER".equals(approverType.trim())) {
            if (node.getApproverRefId() == null) {
                throw new BusinessException(ResultCode.PROCESS_NODE_APPROVER_REQUIRED);
            }
            approverUserIds.add(node.getApproverRefId());
        }
    }

    /**
     * 递归写入一条作用域内的节点链。
     *
     * @param startSort 该链在作用域内的起始排序（主链/分支链均从 0 开始）
     */
    private void persistChain(Long processId, List<ProcessNodeSaveRequest> chain, Long parentId,
                              String branchGroup, int startSort, Set<Long> validUserIds) {
        int sort = startSort;
        for (ProcessNodeSaveRequest dto : nullSafe(chain)) {
            if (TYPE_GATEWAY.equals(dto.getType())) {
                sort = persistGateway(processId, dto, parentId, branchGroup, sort, validUserIds);
            } else {
                validateApproval(dto, new HashSet<>());
                if ("USER".equals(dto.getApproverType().trim())
                        && !validUserIds.contains(dto.getApproverRefId())) {
                    throw new BusinessException(ResultCode.PROCESS_NODE_APPROVER_NOT_FOUND);
                }
                ProcessNode row = new ProcessNode();
                row.setProcessId(processId);
                row.setNodeType(TYPE_APPROVAL);
                row.setParentId(parentId);
                row.setBranchGroup(branchGroup);
                row.setNodeName(dto.getNodeName().trim());
                row.setApproverType(dto.getApproverType().trim());
                row.setApproverRefId("USER".equals(dto.getApproverType().trim()) ? dto.getApproverRefId() : null);
                row.setSort(sort);
                nodeMapper.insert(row);
                sort++;
            }
        }
    }

    /** 写入一个网关（分叉行 + 每分支锚点/子链 + 汇聚行），返回该网关在父链中占用排序后的下一个 sort */
    private int persistGateway(Long processId, ProcessNodeSaveRequest dto, Long parentId,
                               String branchGroup, int forkSort, Set<Long> validUserIds) {
        // 1. 分叉行
        ProcessNode fork = new ProcessNode();
        fork.setProcessId(processId);
        fork.setNodeType(ROW_FORK);
        fork.setGatewayMode(dto.getGatewayMode().trim());
        fork.setParentId(parentId);
        fork.setBranchGroup(branchGroup);
        fork.setNodeName(StringUtils.hasText(dto.getNodeName()) ? dto.getNodeName().trim() : defaultForkName(dto));
        fork.setSort(forkSort);
        nodeMapper.insert(fork);

        // 2. 逐分支：锚点行 + 分支内子链
        List<BranchSaveRequest> branches = dto.getBranches();
        Long joinId = null;
        for (int bi = 0; bi < branches.size(); bi++) {
            BranchSaveRequest branch = branches.get(bi);
            String group = "g" + fork.getId() + "_" + bi;

            ProcessNode anchor = new ProcessNode();
            anchor.setProcessId(processId);
            anchor.setNodeType(ROW_BRANCH);
            anchor.setParentId(fork.getId());
            anchor.setBranchGroup(group);
            anchor.setNodeName(StringUtils.hasText(branch.getConditionName()) ? branch.getConditionName().trim() : "");
            anchor.setConditionExpr(StringUtils.hasText(branch.getConditionExpr()) ? branch.getConditionExpr().trim() : null);
            anchor.setSort(BRANCH_ANCHOR_SORT_BASE + bi);
            nodeMapper.insert(anchor);

            persistChain(processId, branch.getNodes(), fork.getId(), group, 0, validUserIds);
        }

        // 3. 汇聚行（所有分支共享一条，挂在分叉作用域、无分支组）
        ProcessNode join = new ProcessNode();
        join.setProcessId(processId);
        join.setNodeType(ROW_JOIN);
        join.setParentId(fork.getId());
        join.setBranchGroup(null);
        join.setNodeName("合流");
        join.setSort(JOIN_SORT);
        nodeMapper.insert(join);
        joinId = join.getId();

        // 4. 回填配对关系
        fork.setJoinNodeId(joinId);
        nodeMapper.updateById(fork);

        return forkSort + 1;
    }

    private String defaultForkName(ProcessNodeSaveRequest dto) {
        return MODE_PARALLEL.equals(dto.getGatewayMode().trim()) ? "并行分支" : "条件分支";
    }

    private List<ProcessNodeSaveRequest> nullSafe(List<ProcessNodeSaveRequest> chain) {
        return chain == null ? new ArrayList<>() : chain;
    }

    // ---------------- 回显（行 → 树） ----------------

    private ProcessDraftVO toDraftVO(ProcessDefinition process) {
        ProcessDraftVO vo = new ProcessDraftVO();
        vo.setId(process.getId());
        vo.setProcessName(process.getProcessName());
        vo.setProcessKey(process.getProcessKey());
        vo.setStatus(process.getStatus());
        vo.setRemark(process.getRemark());
        vo.setCreateTime(process.getCreateTime());
        vo.setUpdateTime(process.getUpdateTime());

        List<ProcessNode> rows = nodeMapper.selectList(
                new LambdaQueryWrapper<ProcessNode>()
                        .eq(ProcessNode::getProcessId, process.getId()));

        // 批量查询审批人姓名，避免 N+1
        Set<Long> approverIds = rows.stream()
                .filter(r -> TYPE_APPROVAL.equals(r.getNodeType()))
                .map(ProcessNode::getApproverRefId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> approverNameMap = new HashMap<>();
        if (!approverIds.isEmpty()) {
            userMapper.selectBatchIds(approverIds).forEach(u ->
                    approverNameMap.put(u.getId(),
                            StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()));
        }

        vo.setNodes(buildChain(rows, ROOT_PARENT_ID, null, approverNameMap));
        return vo;
    }

    /**
     * 解析某个作用域（parentId + branchGroup；主链为 0/null）内的节点链。
     * 分支锚点行与该分支内子节点同作用域，但锚点是分支元数据（由 buildGateway 单独读取），
     * 汇聚行不属于任何分支组，二者均不进节点链。
     */
    private List<ProcessNodeVO> buildChain(List<ProcessNode> rows, Long parentId, String branchGroup,
                                           Map<Long, String> approverNameMap) {
        return rows.stream()
                .filter(r -> Objects.equals(r.getParentId(), parentId))
                .filter(r -> Objects.equals(r.getBranchGroup(), branchGroup))
                .filter(r -> !ROW_BRANCH.equals(r.getNodeType()))
                .filter(r -> !ROW_JOIN.equals(r.getNodeType()))
                .sorted(Comparator.comparing(ProcessNode::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProcessNode::getId))
                .map(r -> ROW_FORK.equals(r.getNodeType())
                        ? buildGateway(r, rows, approverNameMap)
                        : buildApproval(r, approverNameMap))
                .toList();
    }

    private ProcessNodeVO buildApproval(ProcessNode row, Map<Long, String> approverNameMap) {
        ProcessNodeVO vo = new ProcessNodeVO();
        vo.setId(row.getId());
        vo.setType(TYPE_APPROVAL);
        vo.setNodeName(row.getNodeName());
        vo.setApproverType(row.getApproverType());
        vo.setApproverRefId(row.getApproverRefId());
        if (row.getApproverRefId() != null) {
            vo.setApproverRefName(approverNameMap.get(row.getApproverRefId()));
        }
        return vo;
    }

    private ProcessNodeVO buildGateway(ProcessNode forkRow, List<ProcessNode> rows,
                                       Map<Long, String> approverNameMap) {
        ProcessNodeVO vo = new ProcessNodeVO();
        vo.setId(forkRow.getId());
        vo.setType(TYPE_GATEWAY);
        vo.setNodeName(forkRow.getNodeName());
        vo.setGatewayMode(forkRow.getGatewayMode());
        vo.setJoinId(forkRow.getJoinNodeId());

        // 锚点行按 sort 决定分支顺序，每个锚点的 branchGroup 即该分支作用域
        List<ProcessNode> anchors = rows.stream()
                .filter(r -> ROW_BRANCH.equals(r.getNodeType()))
                .filter(r -> Objects.equals(r.getParentId(), forkRow.getId()))
                .sorted(Comparator.comparing(ProcessNode::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProcessNode::getId))
                .toList();

        List<BranchVO> branches = new ArrayList<>();
        for (ProcessNode anchor : anchors) {
            BranchVO branch = new BranchVO();
            branch.setBranchId(anchor.getId());
            branch.setConditionName(anchor.getNodeName());
            branch.setConditionExpr(anchor.getConditionExpr());
            branch.setNodes(buildChain(rows, forkRow.getId(), anchor.getBranchGroup(), approverNameMap));
            branches.add(branch);
        }
        vo.setBranches(branches);
        return vo;
    }

    // ---------------- 流程主表 ----------------

    /** 按固定 key 查询主干流程；不存在则创建，并发首次访问时依赖唯一键兜底后重查 */
    private ProcessDefinition getOrCreateDefaultProcess() {
        ProcessDefinition process = processMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinition>().eq(ProcessDefinition::getProcessKey, DEFAULT_PROCESS_KEY));
        if (process != null) {
            return process;
        }

        process = new ProcessDefinition();
        process.setProcessName(DEFAULT_PROCESS_NAME);
        process.setProcessKey(DEFAULT_PROCESS_KEY);
        process.setStatus(STATUS_DRAFT);
        process.setCreateBy(UserContext.getCurrentUserId());
        process.setUpdateBy(UserContext.getCurrentUserId());
        try {
            processMapper.insert(process);
        } catch (DuplicateKeyException e) {
            // 另一管理员已先行创建：重查即可
            process = processMapper.selectOne(new LambdaQueryWrapper<ProcessDefinition>()
                    .eq(ProcessDefinition::getProcessKey, DEFAULT_PROCESS_KEY));
            if (process == null) {
                throw e;
            }
        }
        return process;
    }
}
