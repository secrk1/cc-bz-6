package com.workflow.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.approval.entity.ProcessDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinition> {
}
