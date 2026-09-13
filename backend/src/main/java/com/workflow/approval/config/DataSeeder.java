package com.workflow.approval.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workflow.approval.entity.Dept;
import com.workflow.approval.entity.SysUser;
import com.workflow.approval.mapper.DeptMapper;
import com.workflow.approval.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 内置数据播种（独立 Bean，保证 @Transactional 经过 Spring 代理生效）：
 * 1. 系统超级管理员 admin / admin123（不归属部门）
 * 2. 四个一级部门：信息科技部、平台业务部、人力资源部、设计部
 * 3. 每个部门 1 名部门管理员（任负责人，角色 ADMIN）+ 2 名普通用户（角色 USER），密码均为 12345678
 * <p>
 * 全部按账号/部门名判重，重复执行（重启）幂等，不覆盖线上已调整的数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String SEED_USER_PASSWORD = "12345678";
    private static final long ROOT_PARENT_ID = 0L;

    /** 部门种子：部门名 / 部门管理员账号 / 两名普通用户账号 */
    private static final List<DeptSeed> DEPT_SEEDS = List.of(
            new DeptSeed("信息科技部", "it_admin", "it_user1", "it_user2"),
            new DeptSeed("平台业务部", "biz_admin", "biz_user1", "biz_user2"),
            new DeptSeed("人力资源部", "hr_admin", "hr_user1", "hr_user2"),
            new DeptSeed("设计部", "design_admin", "design_user1", "design_user2")
    );

    private final SysUserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PasswordEncoder encoder;

    @Transactional(rollbackFor = Exception.class)
    public void seed() {
        initSystemAdmin();
        for (int i = 0; i < DEPT_SEEDS.size(); i++) {
            initDeptWithUsers(DEPT_SEEDS.get(i), i + 1);
        }
    }

    private void initSystemAdmin() {
        if (existsUser(DEFAULT_ADMIN_USERNAME)) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(encoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("内置系统管理员初始化完成: {}", DEFAULT_ADMIN_USERNAME);
    }

    private void initDeptWithUsers(DeptSeed seed, int sort) {
        Dept existing = deptMapper.selectOne(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getParentId, ROOT_PARENT_ID)
                .eq(Dept::getDeptName, seed.deptName()));

        boolean isNew = existing == null;
        Dept dept = existing;
        if (isNew) {
            dept = new Dept();
            dept.setParentId(ROOT_PARENT_ID);
            dept.setDeptName(seed.deptName());
            dept.setSort(sort);
            dept.setStatus(1);
            deptMapper.insert(dept);
            log.info("内置部门初始化完成: {}", seed.deptName());
        }

        // 部门管理员（负责人）
        SysUser leader = createUserIfAbsent(
                seed.adminUsername(), seed.deptName() + "管理员", dept.getId(), "ADMIN");
        // 两名普通用户
        createUserIfAbsent(seed.user1(), seed.deptName() + "员工A", dept.getId(), "USER");
        createUserIfAbsent(seed.user2(), seed.deptName() + "员工B", dept.getId(), "USER");

        // 新建部门时回填负责人；已存在部门保持人工调整结果
        if (isNew) {
            dept.setLeaderId(leader.getId());
            deptMapper.updateById(dept);
        }
    }

    private SysUser createUserIfAbsent(String username, String nickname, Long deptId, String role) {
        SysUser existing = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (existing != null) {
            return existing;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(SEED_USER_PASSWORD));
        user.setNickname(nickname);
        user.setEmail(username + "@example.com");
        user.setDeptId(deptId);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("内置用户初始化完成: {} ({})", username, role);
        return user;
    }

    private boolean existsUser(String username) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return count != null && count > 0;
    }

    /** 部门种子定义 */
    private record DeptSeed(String deptName, String adminUsername, String user1, String user2) {
    }
}
