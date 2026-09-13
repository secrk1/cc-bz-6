package com.workflow.approval.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 流程节点表结构迁移：兼容仅包含旧版线性链路结构（无 node_type/parent_id 等列）的数据卷。
 * 全新部署由 db/init.sql 直接建出新表，此处全部为“列/索引不存在才执行”的幂等操作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final String TABLE = "wf_process_node";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists(TABLE)) {
            // 新环境可能尚未执行 init.sql（无 MySQL 持久卷时不会出现），跳过即可
            log.debug("表 {} 不存在，跳过结构迁移", TABLE);
            return;
        }

        addColumnIfMissing("node_type",
                "ALTER TABLE wf_process_node ADD COLUMN node_type VARCHAR(20) NOT NULL DEFAULT 'APPROVAL' "
                        + "COMMENT '节点类型：APPROVAL-审批，GATEWAY_FORK-分叉，GATEWAY_BRANCH-分支锚点，GATEWAY_JOIN-汇聚'");
        addColumnIfMissing("gateway_mode",
                "ALTER TABLE wf_process_node ADD COLUMN gateway_mode VARCHAR(16) DEFAULT NULL "
                        + "COMMENT '网关模式（仅分叉节点）：PARALLEL-并行，EXCLUSIVE-互斥'");
        addColumnIfMissing("join_node_id",
                "ALTER TABLE wf_process_node ADD COLUMN join_node_id BIGINT UNSIGNED DEFAULT NULL "
                        + "COMMENT '分叉节点配对的汇聚节点ID'");
        addColumnIfMissing("parent_id",
                "ALTER TABLE wf_process_node ADD COLUMN parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 "
                        + "COMMENT '父容器ID：0-主链，否则为分叉节点ID'");
        addColumnIfMissing("branch_group",
                "ALTER TABLE wf_process_node ADD COLUMN branch_group VARCHAR(40) DEFAULT NULL "
                        + "COMMENT '分支组标识'");
        addColumnIfMissing("condition_expr",
                "ALTER TABLE wf_process_node ADD COLUMN condition_expr VARCHAR(255) DEFAULT NULL "
                        + "COMMENT '分支条件表达式（互斥网关用）'");

        // 旧表 approver_type 为 NOT NULL DEFAULT 'USER'，新拓扑中网关行没有审批类别，放宽为可空（已是可空则跳过）
        if ("NO".equals(columnNullable("approver_type"))) {
            log.info("表 {} 放宽列 approver_type 为可空", TABLE);
            jdbcTemplate.execute("ALTER TABLE wf_process_node MODIFY COLUMN approver_type VARCHAR(32) DEFAULT NULL "
                    + "COMMENT '审批人类别（仅审批节点）：USER/DEPT_LEADER/ROLE_ADMIN'");
        }

        addIndexIfMissing("idx_process_scope",
                "ALTER TABLE wf_process_node ADD INDEX idx_process_scope (process_id, parent_id, branch_group, sort)");
    }

    private boolean tableExists(String tableName) {
        // 按表名判断（应用使用独立库实例）；不使用 DATABASE() 以兼容测试环境的内嵌数据库
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE lower(table_name) = ?",
                Integer.class, tableName.toLowerCase());
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns "
                        + "WHERE lower(table_name) = ? AND lower(column_name) = ?",
                Integer.class, TABLE.toLowerCase(), column.toLowerCase());
        if (count == null || count == 0) {
            log.info("表 {} 新增列: {}", TABLE, column);
            jdbcTemplate.execute(ddl);
        }
    }

    private String columnNullable(String column) {
        return jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE lower(table_name) = ? AND lower(column_name) = ?",
                String.class, TABLE.toLowerCase(), column.toLowerCase());
    }

    private void addIndexIfMissing(String indexName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, TABLE, indexName);
            if (count == null || count == 0) {
                log.info("表 {} 新增索引: {}", TABLE, indexName);
                jdbcTemplate.execute(ddl);
            }
        } catch (Exception e) {
            // information_schema.statistics 为 MySQL 专有视图；其他数据库（如测试环境 H2）直接跳过
            log.debug("索引[{}]存在性检查跳过: {}", indexName, e.getMessage());
        }
    }
}
