import { formBindingData, FormExtensionAbility, formInfo, formProvider } from '@kit.FormKit'
import { Want } from '@kit.AbilityKit'
import { FormCardStore } from '../session/FormCardStore'

/**
 * 桌面 2×2 服务卡片：仅展示备餐中 / 可取餐提示。
 * 数据由主进程 FormCardUpdater 写入 Preferences 后 updateForm。
 */
export default class MiyfFormAbility extends FormExtensionAbility {
  onAddForm(want: Want): formBindingData.FormBindingData {
    const formId = this.readFormId(want)
    if (formId.length > 0) {
      void FormCardStore.addFormId(this.context, formId).then(() => {
        void FormCardStore.load(this.context).then(() => {
          const data = formBindingData.createFormBindingData(FormCardStore.snapshotOrEmpty())
          formProvider.updateForm(formId, data).catch(() => {
          })
        })
      })
    }
    return formBindingData.createFormBindingData(FormCardStore.snapshotOrEmpty())
  }

  onUpdateForm(formId: string): void {
    void FormCardStore.load(this.context).then(() => {
      const data = formBindingData.createFormBindingData(FormCardStore.snapshotOrEmpty())
      formProvider.updateForm(formId, data).catch(() => {
      })
    })
  }

  onRemoveForm(formId: string): void {
    void FormCardStore.removeFormId(this.context, formId)
  }

  onAcquireFormState(_want: Want): formInfo.FormState {
    return formInfo.FormState.READY
  }

  private readFormId(want: Want): string {
    if (want.parameters === undefined) {
      return ''
    }
    const raw = want.parameters[formInfo.FormParam.IDENTITY_KEY]
    return raw !== undefined ? String(raw) : ''
  }
}
