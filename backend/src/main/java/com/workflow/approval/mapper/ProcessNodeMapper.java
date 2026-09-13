package com.workflow.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.approval.entity.ProcessNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessNodeMapper extends BaseMapper<ProcessNode> {
}
