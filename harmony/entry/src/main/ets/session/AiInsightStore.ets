import { OrderInsightAiResult } from '../api/Models'

/** 食客 AI 点菜后的饮食参考卡，预约页读取；提交或清空草稿时清除。 */
export class AiInsightStore {
  private static insight: OrderInsightAiResult | null = null
  private static dinerText: string = ''

  static set(insight: OrderInsightAiResult | null, dinerText: string = ''): void {
    AiInsightStore.insight = insight
    AiInsightStore.dinerText = dinerText
  }

  static get(): OrderInsightAiResult | null {
    return AiInsightStore.insight
  }

  static getDinerText(): string {
    return AiInsightStore.dinerText
  }

  static clear(): void {
    AiInsightStore.insight = null
    AiInsightStore.dinerText = ''
  }
}
