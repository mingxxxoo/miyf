package cn.miyf.workflow;

/**
 * 流程完成结果：通过或驳回。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
public enum WorkflowDecision {
    /** 审核通过 */
    APPROVED,
    /** 审核驳回 */
    REJECTED
}
