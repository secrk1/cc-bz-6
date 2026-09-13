package com.workflow.approval.controller;

import com.workflow.approval.common.Result;
import com.workflow.approval.dto.DraftSaveRequest;
import com.workflow.approval.dto.ProcessDraftVO;
import com.workflow.approval.security.RequireRole;
import com.workflow.approval.service.ProcessDesignerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程设计器：仅流程管理员（ADMIN）可访问，
 * 由类级 {@link RequireRole} 经 RoleAspect 统一拦截，非管理员调用一律 403。
 */
@RestController
@RequestMapping("/api/process-designer")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class ProcessDesignerController {

    private final ProcessDesignerService processDesignerService;

    /** 回显画布草稿（首次访问自动创建仅含开始/结束的默认草稿） */
    @GetMapping("/draft")
    public Result<ProcessDraftVO> getDraft() {
        return Result.success(processDesignerService.getOrCreateDraft());
    }

    /** 保存画布草稿（流程主信息 + 审批节点链全量覆盖） */
    @PutMapping("/draft")
    public Result<ProcessDraftVO> saveDraft(@Valid @RequestBody DraftSaveRequest request) {
        return Result.success(processDesignerService.saveDraft(request));
    }
}
