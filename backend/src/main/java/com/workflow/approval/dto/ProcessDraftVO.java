package com.workflow.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程画布草稿回显结构
 */
@Data
public class ProcessDraftVO {

    private Long id;

    private String processName;

    private String processKey;

    /** DRAFT-草稿 PUBLISHED-已发布 */
    private String status;

    private String remark;

    /** 开始/结束之间的审批节点，已按 sort 升序 */
    private List<ProcessNodeVO> nodes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
