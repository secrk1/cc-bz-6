package com.workflow.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workflow.approval.common.Result;
import com.workflow.approval.dto.PageResult;
import com.workflow.approval.dto.UserOptionVO;
import com.workflow.approval.dto.UserSaveRequest;
import com.workflow.approval.dto.UserVO;
import com.workflow.approval.entity.SysUser;
import com.workflow.approval.mapper.SysUserMapper;
import com.workflow.approval.security.RequireRole;
import com.workflow.approval.security.UserContext;
import com.workflow.approval.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class UserController {

    private final SysUserMapper userMapper;
    private final SysUserService userService;

    /**
     * 人员分页查询，可按关键字（用户名/昵称/手机/邮箱）、角色、部门过滤
     */
    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Long deptId) {
        return Result.success(userService.pageUsers(current, size, keyword, role, deptId));
    }

    /**
     * 新增人员
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserSaveRequest request) {
        return Result.success(userService.createUser(request));
    }

    /**
     * 编辑人员
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserSaveRequest request) {
        userService.updateUser(request);
        return Result.success();
    }

    /**
     * 删除人员
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id, UserContext.getCurrentUserId());
        return Result.success();
    }

    /**
     * 用户下拉选项（部门维护时选择负责人）。可按部门过滤：/api/users/options?deptId=1
     */
    @GetMapping("/options")
    public Result<List<UserOptionVO>> options(@RequestParam(required = false) Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getDeptId)
                .orderByAsc(SysUser::getId);
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        List<UserOptionVO> list = userMapper.selectList(wrapper).stream().map(u -> {
            UserOptionVO vo = new UserOptionVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).toList();
        return Result.success(list);
    }
}
