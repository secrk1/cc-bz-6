package com.workflow.approval.service;

import com.workflow.approval.dto.BranchSaveRequest;
import com.workflow.approval.dto.DraftSaveRequest;
import com.workflow.approval.dto.ProcessDraftVO;
import com.workflow.approval.dto.ProcessNodeSaveRequest;
import com.workflow.approval.dto.ProcessNodeVO;
import com.workflow.approval.entity.SysUser;
import com.workflow.approval.exception.BusinessException;
import com.workflow.approval.mapper.ProcessNodeMapper;
import com.workflow.approval.mapper.SysUserMapper;
import com.workflow.approval.security.LoginUser;
import com.workflow.approval.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 流程草稿拓扑：树 → 扁平行持久化 → 树回显 的集成测试（H2 内存库 + 真实 MyBatis/事务）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProcessDesignerServiceTest {

    @Autowired
    private ProcessDesignerService designerService;

    @Autowired
    private ProcessNodeMapper nodeMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long approverUserId;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(1L, "admin", "ADMIN"));

        SysUser approver = new SysUser();
        approver.setUsername("approver1");
        approver.setPassword(passwordEncoder.encode("12345678"));
        approver.setNickname("审批人甲");
        approver.setRole("USER");
        approver.setStatus(1);
        userMapper.insert(approver);
        approverUserId = approver.getId();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ProcessNodeSaveRequest approval(String name) {
        ProcessNodeSaveRequest n = new ProcessNodeSaveRequest();
        n.setType("APPROVAL");
        n.setNodeName(name);
        n.setApproverType("DEPT_LEADER");
        return n;
    }

    private ProcessNodeSaveRequest userApproval(String name) {
        ProcessNodeSaveRequest n = approval(name);
        n.setApproverType("USER");
        n.setApproverRefId(approverUserId);
        return n;
    }

    private BranchSaveRequest branch(String conditionName, List<ProcessNodeSaveRequest> nodes) {
        BranchSaveRequest b = new BranchSaveRequest();
        b.setConditionName(conditionName);
        b.setConditionExpr("amount > 1000");
        b.setNodes(nodes);
        return b;
    }

    private ProcessNodeSaveRequest gateway(String mode, List<BranchSaveRequest> branches) {
        ProcessNodeSaveRequest g = new ProcessNodeSaveRequest();
        g.setType("GATEWAY");
        g.setGatewayMode(mode);
        g.setBranches(branches);
        return g;
    }

    @Test
    void firstGet_shouldCreateEmptyDraft() {
        ProcessDraftVO draft = designerService.getOrCreateDraft();
        assertNotNull(draft.getId());
        assertEquals("主干审批流程", draft.getProcessName());
        assertEquals("DRAFT", draft.getStatus());
        assertTrue(draft.getNodes().isEmpty());
    }

    @Test
    void saveNestedTopology_shouldPersistAndEchoTree() {
        // 主链：A → 互斥网关(B | C→并行网关(E|F)) → D
        ProcessNodeSaveRequest nestedParallel = gateway("PARALLEL", List.of(
                branch(null, List.of(approval("E"))),
                branch(null, List.of(approval("F")))
        ));
        ProcessNodeSaveRequest outerExclusive = gateway("EXCLUSIVE", List.of(
                branch("金额>1000", List.of(approval("B"))),
                branch("金额<=1000", new ArrayList<>(List.of(approval("C"), nestedParallel)))
        ));
        List<ProcessNodeSaveRequest> root = new ArrayList<>(List.of(
                userApproval("A"), outerExclusive, approval("D")));

        DraftSaveRequest request = new DraftSaveRequest();
        request.setProcessName("嵌套分支测试流程");
        request.setNodes(root);

        ProcessDraftVO echo = designerService.saveDraft(request);

        // ---- 回显树断言 ----
        assertEquals("嵌套分支测试流程", echo.getProcessName());
        List<ProcessNodeVO> chain = echo.getNodes();
        assertEquals(3, chain.size());
        assertEquals("A", chain.get(0).getNodeName());
        assertEquals("APPROVAL", chain.get(0).getType());
        assertEquals("审批人甲", chain.get(0).getApproverRefName());

        ProcessNodeVO outer = chain.get(1);
        assertEquals("GATEWAY", outer.getType());
        assertEquals("EXCLUSIVE", outer.getGatewayMode());
        assertNotNull(outer.getId());
        assertNotNull(outer.getJoinId());
        assertNotEquals(outer.getId(), outer.getJoinId());
        assertEquals(2, outer.getBranches().size());
        assertEquals("金额>1000", outer.getBranches().get(0).getConditionName());
        assertEquals("amount > 1000", outer.getBranches().get(0).getConditionExpr());

        List<ProcessNodeVO> branch0 = outer.getBranches().get(0).getNodes();
        assertEquals(1, branch0.size());
        assertEquals("B", branch0.get(0).getNodeName());

        List<ProcessNodeVO> branch1 = outer.getBranches().get(1).getNodes();
        assertEquals(2, branch1.size());
        assertEquals("C", branch1.get(0).getNodeName());
        ProcessNodeVO inner = branch1.get(1);
        assertEquals("GATEWAY", inner.getType());
        assertEquals("PARALLEL", inner.getGatewayMode());
        assertNotNull(inner.getJoinId());
        assertEquals(2, inner.getBranches().size());
        assertEquals("E", inner.getBranches().get(0).getNodes().get(0).getNodeName());
        assertEquals("F", inner.getBranches().get(1).getNodes().get(0).getNodeName());

        assertEquals("D", chain.get(2).getNodeName());

        // ---- 扁平行断言：6 审批 + 2 分叉 + 2 汇聚 + 4 分支锚点 ----
        Long processId = echo.getId();
        assertEquals(6L, countByType(processId, "APPROVAL"));
        assertEquals(2L, countByType(processId, "GATEWAY_FORK"));
        assertEquals(2L, countByType(processId, "GATEWAY_JOIN"));
        assertEquals(4L, countByType(processId, "GATEWAY_BRANCH"));

        // 主链 3 个元素（A、外分叉、D）均 parent_id=0 且无分支组
        assertEquals(3L, jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wf_process_node WHERE process_id=? AND parent_id=0 AND branch_group IS NULL",
                Long.class, processId));

        // 外分叉的两个分支组，组内分别有 1 个、2 个非锚点节点
        Long outerForkId = outer.getId();
        assertEquals(1L, countScopeNodes(processId, outerForkId, "g" + outerForkId + "_0"));
        assertEquals(2L, countScopeNodes(processId, outerForkId, "g" + outerForkId + "_1"));

        // 每个分叉行的 join_node_id 必须指向一条真实汇聚行
        Integer danglingJoin = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wf_process_node f WHERE f.process_id=? AND f.node_type='GATEWAY_FORK' "
                        + "AND NOT EXISTS (SELECT 1 FROM wf_process_node j WHERE j.id=f.join_node_id "
                        + "AND j.node_type='GATEWAY_JOIN')",
                Integer.class, processId);
        assertEquals(0, danglingJoin);

        // 重复回显应稳定（再次查询树结构一致）
        ProcessDraftVO reread = designerService.getOrCreateDraft();
        assertEquals(3, reread.getNodes().size());
        assertEquals(2, reread.getNodes().get(1).getBranches().size());
    }

    @Test
    void saveAgain_shouldFullyRebuild() {
        DraftSaveRequest first = new DraftSaveRequest();
        first.setProcessName("第一次保存");
        first.setNodes(new ArrayList<>(List.of(
                approval("A"),
                gateway("PARALLEL", List.of(
                        branch(null, List.of(approval("B"))),
                        branch(null, List.of(approval("C")))
                )))));
        ProcessDraftVO saved = designerService.saveDraft(first);
        // A/B/C 审批 + 1 分叉 + 1 汇聚 + 2 分支锚点 = 7 行
        assertEquals(7L, countByType(saved.getId(), "APPROVAL")
                + countByType(saved.getId(), "GATEWAY_FORK")
                + countByType(saved.getId(), "GATEWAY_JOIN")
                + countByType(saved.getId(), "GATEWAY_BRANCH"));

        DraftSaveRequest second = new DraftSaveRequest();
        second.setProcessName("第二次保存");
        second.setNodes(new ArrayList<>(List.of(approval("X"))));
        ProcessDraftVO rebuilt = designerService.saveDraft(second);

        assertEquals("第二次保存", rebuilt.getProcessName());
        assertEquals(1, rebuilt.getNodes().size());
        assertEquals("X", rebuilt.getNodes().get(0).getNodeName());
        // 旧拓扑（网关、汇聚、锚点）必须随全量重建清空
        assertEquals(1L, countByType(rebuilt.getId(), "APPROVAL"));
        assertEquals(0L, countByType(rebuilt.getId(), "GATEWAY_FORK"));
        assertEquals(0L, countByType(rebuilt.getId(), "GATEWAY_JOIN"));
        assertEquals(0L, countByType(rebuilt.getId(), "GATEWAY_BRANCH"));
    }

    @Test
    void gatewayWithSingleBranch_shouldReject() {
        DraftSaveRequest req = new DraftSaveRequest();
        req.setProcessName("非法分支数");
        req.setNodes(new ArrayList<>(List.of(
                gateway("PARALLEL", List.of(branch(null, List.of(approval("B"))))))));
        BusinessException ex = assertThrows(BusinessException.class, () -> designerService.saveDraft(req));
        assertEquals(3008, ex.getCode());
    }

    @Test
    void exclusiveWithoutConditionName_shouldReject() {
        DraftSaveRequest req = new DraftSaveRequest();
        req.setProcessName("缺条件名");
        req.setNodes(new ArrayList<>(List.of(
                gateway("EXCLUSIVE", List.of(
                        branch(null, List.of(approval("B"))),
                        branch(null, List.of(approval("C")))
                )))));
        BusinessException ex = assertThrows(BusinessException.class, () -> designerService.saveDraft(req));
        assertEquals(3009, ex.getCode());
    }

    @Test
    void blankApprovalName_shouldReject() {
        DraftSaveRequest req = new DraftSaveRequest();
        req.setProcessName("空节点名");
        ProcessNodeSaveRequest noName = approval("   ");
        req.setNodes(new ArrayList<>(List.of(noName)));
        BusinessException ex = assertThrows(BusinessException.class, () -> designerService.saveDraft(req));
        assertEquals(3002, ex.getCode());
    }

    @Test
    void userApproverMissing_shouldReject() {
        DraftSaveRequest req = new DraftSaveRequest();
        req.setProcessName("审批人不存在");
        ProcessNodeSaveRequest node = approval("幽灵审批");
        node.setApproverType("USER");
        node.setApproverRefId(999999L);
        req.setNodes(new ArrayList<>(List.of(node)));
        BusinessException ex = assertThrows(BusinessException.class, () -> designerService.saveDraft(req));
        assertEquals(3005, ex.getCode());
    }

    private long countByType(Long processId, String type) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wf_process_node WHERE process_id=? AND node_type=?",
                Long.class, processId, type);
    }

    private long countScopeNodes(Long processId, Long parentId, String group) {
        // 分支组内非锚点节点数（审批行 + 嵌套分叉行；锚点为 GATEWAY_BRANCH，汇聚挂在内层分叉下）
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wf_process_node WHERE process_id=? AND parent_id=? AND branch_group=? "
                        + "AND node_type IN ('APPROVAL','GATEWAY_FORK')",
                Long.class, processId, parentId, group);
    }
}
