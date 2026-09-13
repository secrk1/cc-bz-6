package com.workflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workflow.approval.common.ResultCode;
import com.workflow.approval.dto.PageResult;
import com.workflow.approval.dto.UserSaveRequest;
import com.workflow.approval.dto.UserVO;
import com.workflow.approval.entity.Dept;
import com.workflow.approval.entity.SysUser;
import com.workflow.approval.exception.BusinessException;
import com.workflow.approval.mapper.DeptMapper;
import com.workflow.approval.mapper.SysUserMapper;
import com.workflow.approval.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> pageUsers(long current, long size, String keyword, String role, Long deptId) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getNickname, kw)
                    .or().like(SysUser::getPhone, kw)
                    .or().like(SysUser::getEmail, kw));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        wrapper.orderByAsc(SysUser::getDeptId).orderByAsc(SysUser::getId);

        Page<SysUser> result = userMapper.selectPage(page, wrapper);

        // 批量填充部门名称，避免 N+1
        Map<Long, String> deptNameMap = new HashMap<>();
        Set<Long> deptIds = result.getRecords().stream()
                .map(SysUser::getDeptId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!deptIds.isEmpty()) {
            deptMapper.selectBatchIds(deptIds)
                    .forEach(d -> deptNameMap.put(d.getId(), d.getDeptName()));
        }

        List<UserVO> records = result.getRecords().stream().map(u -> toVO(u, deptNameMap)).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveRequest request) {
        validateUsernameUnique(request.getUsername().trim(), null);
        validateDeptExists(request.getDeptId());

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(trimToNull(request.getNickname()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setRole(request.getRole());
        user.setDeptId(request.getDeptId());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户ID不能为空");
        }
        SysUser existing = userMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 用户名不允许修改（登录账号保持稳定），以库中账号为准
        validateDeptExists(request.getDeptId());

        // 调岗/移出部门：若该用户是某部门负责人，禁止变更部门，必须先在部门管理更换负责人
        Long oldDeptId = existing.getDeptId();
        Long newDeptId = request.getDeptId();
        boolean deptChanged = !java.util.Objects.equals(oldDeptId, newDeptId);
        if (deptChanged && isLeaderOfAnyDept(request.getId(), null)) {
            throw new BusinessException(ResultCode.USER_IS_DEPT_LEADER);
        }

        existing.setNickname(trimToNull(request.getNickname()));
        existing.setPhone(trimToNull(request.getPhone()));
        existing.setEmail(trimToNull(request.getEmail()));
        existing.setRole(request.getRole());
        existing.setDeptId(newDeptId);
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        // 密码留空表示不修改
        if (StringUtils.hasText(request.getPassword())) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id, Long operatorId) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (operatorId != null && operatorId.equals(id)) {
            throw new BusinessException(ResultCode.CANNOT_DELETE_SELF);
        }
        // 负责人需先在部门管理中更换，避免部门悬挂一个已删除的负责人
        if (isLeaderOfAnyDept(id, null)) {
            throw new BusinessException(ResultCode.USER_IS_DEPT_LEADER);
        }
        userMapper.deleteById(id);
    }

    // ---------------- 校验 ----------------

    private void validateUsernameUnique(String username, Long currentId) {
        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username);
        if (currentId != null) {
            wrapper.ne(SysUser::getId, currentId);
        }
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.USERNAME_DUPLICATE);
        }
    }

    private void validateDeptExists(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND);
        }
    }

    /** 是否为某个部门的负责人（可排除指定部门） */
    private boolean isLeaderOfAnyDept(Long userId, Long excludeDeptId) {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<Dept>().eq(Dept::getLeaderId, userId);
        if (excludeDeptId != null) {
            wrapper.ne(Dept::getId, excludeDeptId);
        }
        Long count = deptMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private UserVO toVO(SysUser user, Map<Long, String> deptNameMap) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setDeptId(user.getDeptId());
        if (user.getDeptId() != null) {
            vo.setDeptName(deptNameMap.get(user.getDeptId()));
        }
        vo.setStatus(user.getStatus());
        return vo;
    }
}
