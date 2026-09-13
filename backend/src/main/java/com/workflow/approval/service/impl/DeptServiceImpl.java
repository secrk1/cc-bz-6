package com.workflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workflow.approval.common.ResultCode;
import com.workflow.approval.dto.DeptSaveRequest;
import com.workflow.approval.dto.DeptTreeVO;
import com.workflow.approval.entity.Dept;
import com.workflow.approval.entity.SysUser;
import com.workflow.approval.exception.BusinessException;
import com.workflow.approval.mapper.DeptMapper;
import com.workflow.approval.mapper.SysUserMapper;
import com.workflow.approval.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    /** 根部门的虚拟父ID */
    private static final long ROOT_PARENT_ID = 0L;

    private final DeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<DeptTreeVO> getDeptTree() {
        // 一次查询全部部门，按排序号、ID 排序
        List<Dept> allDepts = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSort).orderByAsc(Dept::getId));
        if (allDepts.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询负责人姓名，避免 N+1
        Set<Long> leaderIds = allDepts.stream()
                .map(Dept::getLeaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> leaderNameMap = new HashMap<>();
        if (!leaderIds.isEmpty()) {
            userMapper.selectBatchIds(leaderIds).forEach(u ->
                    leaderNameMap.put(u.getId(),
                            StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()));
        }

        // 内存中递归挂接成树
        Map<Long, DeptTreeVO> nodeMap = new HashMap<>();
        List<DeptTreeVO> roots = new ArrayList<>();
        for (Dept dept : allDepts) {
            DeptTreeVO node = toTreeVO(dept, leaderNameMap);
            nodeMap.put(node.getId(), node);
        }
        for (DeptTreeVO node : nodeMap.values()) {
            if (node.getParentId() != null && node.getParentId() != ROOT_PARENT_ID) {
                DeptTreeVO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 父级缺失（异常数据）时作为根节点兜底展示
                    roots.add(node);
                }
            } else {
                roots.add(node);
            }
        }

        Comparator<DeptTreeVO> comparator =
                Comparator.comparing(DeptTreeVO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DeptTreeVO::getId);
        sortTree(roots, comparator);
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDept(DeptSaveRequest request) {
        validateParent(request.getParentId(), null);
        validateNameUnique(request.getParentId(), request.getDeptName(), null);

        Dept dept = new Dept();
        dept.setParentId(request.getParentId());
        dept.setDeptName(request.getDeptName().trim());
        dept.setLeaderId(request.getLeaderId());
        dept.setSort(request.getSort() == null ? 0 : request.getSort());
        dept.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        dept.setRemark(request.getRemark());
        deptMapper.insert(dept);

        // 负责人归属联动（需要部门已落库以取得 deptId）
        applyLeaderRule(dept.getId(), request.getLeaderId());
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门ID不能为空");
        }
        Dept exists = deptMapper.selectById(request.getId());
        if (exists == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }

        validateParent(request.getParentId(), request.getId());
        validateNameUnique(request.getParentId(), request.getDeptName(), request.getId());

        exists.setParentId(request.getParentId());
        exists.setDeptName(request.getDeptName().trim());
        exists.setLeaderId(request.getLeaderId());
        exists.setSort(request.getSort() == null ? 0 : request.getSort());
        exists.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        exists.setRemark(request.getRemark());
        deptMapper.updateById(exists);

        // 负责人归属联动：无部门自动归入本部门，已属其他部门则拒绝
        applyLeaderRule(request.getId(), request.getLeaderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }

        // 关联校验 1：存在子部门不允许删除
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_CHILDREN);
        }

        // 关联校验 2：部门下仍有用户不允许删除
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_USERS);
        }

        deptMapper.deleteById(id);
    }

    // ---------------- 校验逻辑 ----------------

    /** 校验父部门存在；编辑时禁止挂载到自身或自己的子孙部门下 */
    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上级部门不能为空");
        }
        if (parentId == ROOT_PARENT_ID) {
            return;
        }
        if (deptMapper.selectById(parentId) == null) {
            throw new BusinessException(ResultCode.PARENT_DEPT_NOT_FOUND);
        }
        if (currentId != null) {
            // 沿父链向上查找，若命中当前节点，说明新父级是当前节点自身或其子孙
            Long cursor = parentId;
            int guard = 0;
            while (cursor != null && cursor != ROOT_PARENT_ID && guard++ < 1000) {
                if (cursor.equals(currentId)) {
                    throw new BusinessException(ResultCode.DEPT_PARENT_INVALID);
                }
                Dept parent = deptMapper.selectById(cursor);
                cursor = parent == null ? null : parent.getParentId();
            }
        }
    }

    /** 同一父部门下名称不可重复 */
    private void validateNameUnique(Long parentId, String deptName, Long currentId) {
        if (!StringUtils.hasText(deptName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "部门名称不能为空");
        }
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<Dept>()
                .eq(Dept::getParentId, parentId)
                .eq(Dept::getDeptName, deptName.trim());
        if (currentId != null) {
            wrapper.ne(Dept::getId, currentId);
        }
        Long count = deptMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.DEPT_NAME_DUPLICATE);
        }
    }

    /**
     * 负责人归属联动规则：
     * 1. 不设置负责人：不做处理；
     * 2. 用户不存在：报错；
     * 3. 用户已归属其他部门：拒绝设置为本部门负责人；
     * 4. 用户尚未分配部门：自动将其所属部门设置为本部门。
     */
    private void applyLeaderRule(Long deptId, Long leaderId) {
        if (leaderId == null) {
            return;
        }
        SysUser leader = userMapper.selectById(leaderId);
        if (leader == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), "指定的部门负责人不存在");
        }
        Long userDeptId = leader.getDeptId();
        if (userDeptId != null && !userDeptId.equals(deptId)) {
            throw new BusinessException(ResultCode.LEADER_BELONGS_OTHER_DEPT);
        }
        if (userDeptId == null) {
            SysUser update = new SysUser();
            update.setId(leaderId);
            update.setDeptId(deptId);
            userMapper.updateById(update);
        }
    }

    private void sortTree(List<DeptTreeVO> nodes, Comparator<DeptTreeVO> comparator) {
        nodes.sort(comparator);
        for (DeptTreeVO node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTree(node.getChildren(), comparator);
            }
        }
    }

    private DeptTreeVO toTreeVO(Dept dept, Map<Long, String> leaderNameMap) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setLeaderId(dept.getLeaderId());
        if (dept.getLeaderId() != null) {
            vo.setLeaderName(leaderNameMap.get(dept.getLeaderId()));
        }
        vo.setSort(dept.getSort());
        vo.setStatus(dept.getStatus());
        vo.setRemark(dept.getRemark());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}
