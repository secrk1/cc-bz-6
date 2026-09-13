package com.workflow.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点
 */
@Data
public class DeptTreeVO {

    private Long id;
    private Long parentId;
    private String deptName;
    private Long leaderId;
    private String leaderName;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    private List<DeptTreeVO> children = new ArrayList<>();
}
