package com.workflow.approval.controller;

import com.workflow.approval.common.Result;
import com.workflow.approval.dto.DeptSaveRequest;
import com.workflow.approval.dto.DeptTreeVO;
import com.workflow.approval.security.RequireRole;
import com.workflow.approval.service.DeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 部门树：登录用户均可查看（流程人员寻址也依赖该接口）
     */
    @GetMapping("/tree")
    public Result<List<DeptTreeVO>> tree() {
        return Result.success(deptService.getDeptTree());
    }

    /**
     * 新增部门（仅管理员）
     */
    @PostMapping
    @RequireRole("ADMIN")
    public Result<Long> create(@Valid @RequestBody DeptSaveRequest request) {
        return Result.success(deptService.createDept(request));
    }

    /**
     * 编辑部门（仅管理员）
     */
    @PutMapping
    @RequireRole("ADMIN")
    public Result<Void> update(@Valid @RequestBody DeptSaveRequest request) {
        deptService.updateDept(request);
        return Result.success();
    }

    /**
     * 删除空部门（仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success();
    }
}
