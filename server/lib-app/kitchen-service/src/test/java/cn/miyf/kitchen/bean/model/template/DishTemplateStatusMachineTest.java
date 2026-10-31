package cn.miyf.kitchen.bean.model.template;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜品模板状态机单元测试。
 *
 * @author XieMingJie
 * @since 2026-10-31 14:00
 */
class DishTemplateStatusMachineTest {

    @Test
    void allowsDraftAndPreCreatedToPublish() {
        assertTrue(DishTemplateStatusMachine.canTransit(
                DishTemplateStatus.DRAFT, DishTemplateStatus.PUBLISHED));
        assertTrue(DishTemplateStatusMachine.canTransit(
                DishTemplateStatus.PRE_CREATED, DishTemplateStatus.PUBLISHED));
    }

    @Test
    void allowsPublishedToArchiveAndArchiveBackToDraft() {
        assertTrue(DishTemplateStatusMachine.canTransit(
                DishTemplateStatus.PUBLISHED, DishTemplateStatus.ARCHIVED));
        assertTrue(DishTemplateStatusMachine.canTransit(
                DishTemplateStatus.ARCHIVED, DishTemplateStatus.DRAFT));
    }

    @Test
    void rejectsPublishingArchivedDirectly() {
        assertFalse(DishTemplateStatusMachine.canTransit(
                DishTemplateStatus.ARCHIVED, DishTemplateStatus.PUBLISHED));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DishTemplateStatusMachine.assertTransit(
                        DishTemplateStatus.ARCHIVED, DishTemplateStatus.PUBLISHED));
        assertEquals(ErrorCode.INVALID_STATUS.getCode(), ex.getCode());
    }

    @Test
    void onlyDraftAndPreCreatedAreEditable() {
        assertTrue(DishTemplateStatusMachine.editable(DishTemplateStatus.DRAFT));
        assertTrue(DishTemplateStatusMachine.editable(DishTemplateStatus.PRE_CREATED));
        assertFalse(DishTemplateStatusMachine.editable(DishTemplateStatus.PUBLISHED));
        assertFalse(DishTemplateStatusMachine.editable(DishTemplateStatus.ARCHIVED));
    }

    @Test
    void normalizesStatusIgnoringCaseAndWhitespace() {
        assertEquals(DishTemplateStatus.PRE_CREATED,
                DishTemplateStatus.normalize("  PRE_CREATED "));
        assertTrue(DishTemplateStatus.isValid("DRAFT"));
        assertFalse(DishTemplateStatus.isValid("unknown"));
        assertFalse(DishTemplateStatus.isValid(null));
    }
}
