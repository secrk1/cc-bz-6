package com.workflow.approval.service;

import com.workflow.approval.dto.DraftSaveRequest;
import com.workflow.approval.dto.ProcessDraftVO;

/**
 * 流程设计器：画布草稿的回显与保存
 */
public interface ProcessDesignerService {

    /** 回显草稿；不存在时自动创建一份仅含开始/结束（零审批节点）的默认草稿 */
    ProcessDraftVO getOrCreateDraft();

    /** 全量保存画布草稿（流程主信息 + 审批节点链） */
    ProcessDraftVO saveDraft(DraftSaveRequest request);
}
