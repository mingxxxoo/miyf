import { View, Text, Textarea, Input, Button } from '@tarojs/components'
import './index.scss'

export interface AiPromptSheetProps {
  visible: boolean
  title: string
  hint?: string
  placeholder?: string
  /** 是否展示链接输入（厨师建菜） */
  showUrl?: boolean
  submitting?: boolean
  text: string
  url?: string
  onTextChange: (value: string) => void
  onUrlChange?: (value: string) => void
  onClose: () => void
  onSubmit: () => void
}

/** AI 输入弹层：文本 + 可选链接 */
export default function AiPromptSheet({
  visible,
  title,
  hint,
  placeholder = '用一句话说清楚你的需求…',
  showUrl = false,
  submitting = false,
  text,
  url = '',
  onTextChange,
  onUrlChange,
  onClose,
  onSubmit
}: AiPromptSheetProps) {
  if (!visible) return null

  return (
    <View className='ai-sheet'>
      <View className='ai-sheet__mask' onClick={onClose} />
      <View className='ai-sheet__panel'>
        <View className='ai-sheet__head'>
          <Text className='ai-sheet__title'>{title}</Text>
          <Text className='ai-sheet__close' onClick={onClose}>
            关闭
          </Text>
        </View>
        {hint ? <Text className='ai-sheet__hint'>{hint}</Text> : null}
        <Textarea
          className='ai-sheet__textarea'
          value={text}
          maxlength={2000}
          placeholder={placeholder}
          autoHeight
          onInput={(e) => onTextChange(e.detail.value)}
        />
        {showUrl ? (
          <Input
            className='ai-sheet__url'
            value={url}
            placeholder='或粘贴菜谱网页链接（http/https）'
            onInput={(e) => onUrlChange?.(e.detail.value)}
          />
        ) : null}
        <Button
          className='ai-sheet__submit ck-btn-primary'
          loading={submitting}
          disabled={submitting}
          onClick={onSubmit}
        >
          {submitting ? '识别中…' : '开始识别'}
        </Button>
      </View>
    </View>
  )
}
