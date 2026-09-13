package com.workflow.approval.service;

import com.workflow.approval.dto.DeptSaveRequest;
import com.workflow.approval.dto.DeptTreeVO;

import java.util.List;

public interface DeptService {

    /** 查询完整部门树（递归构建） */
    List<DeptTreeVO> getDeptTree();

    /** 新增部门 */
    Long createDept(DeptSaveRequest request);

    /** 编辑部门 */
    void updateDept(DeptSaveRequest request);

    /** 删除空部门（无子部门、无关联用户） */
    void deleteDept(Long id);
}
