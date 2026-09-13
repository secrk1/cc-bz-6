package com.workflow.approval.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义主表
 */
@Data
@TableName("wf_process_definition")
public class ProcessDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String processName;

    /** 流程唯一标识 */
    private String processKey;

    /** DRAFT-草稿 PUBLISHED-已发布 */
    private String status;

    private String remark;

    private Long createBy;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
