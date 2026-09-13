package com.workflow.approval;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.workflow.approval.mapper")
public class WorkflowApprovalApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApprovalApplication.class, args);
    }
}
