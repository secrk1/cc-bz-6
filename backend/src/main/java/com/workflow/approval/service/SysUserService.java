package com.workflow.approval.service;

import com.workflow.approval.dto.PageResult;
import com.workflow.approval.dto.UserSaveRequest;
import com.workflow.approval.dto.UserVO;

public interface SysUserService {

    /** 分页查询人员，可按关键字/角色/部门过滤 */
    PageResult<UserVO> pageUsers(long current, long size, String keyword, String role, Long deptId);

    /** 新增人员 */
    Long createUser(UserSaveRequest request);

    /** 编辑人员 */
    void updateUser(UserSaveRequest request);

    /** 删除人员 */
    void deleteUser(Long id, Long operatorId);
}
