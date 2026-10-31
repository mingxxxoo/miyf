import {Image, Input, Text, Textarea, View} from '@tarojs/components'
import Taro from '@tarojs/taro'
import {useState} from 'react'
import type {DishTemplateAiResult} from '@/api/dishTemplateAi'
import {extractDishTemplateFromImage, extractDishTemplateFromText} from '@/api/dishTemplateAi'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import {saveDishAiDraft} from '@/utils/dishAiDraft'
import {formatDuration} from '@/utils/format'
import './ai.scss'

type Mode = 'image' | 'text' | 'link'

const MODES: { key: Mode; label: string }[] = [
  { key: 'image', label: '拍照识别' },
  { key: 'text', label: '文字整理' },
  { key: 'link', label: '链接整理' }
]

const CONFIDENCE_LABEL: Record<string, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低'
}

export default function DishAiPage() {
  const [mode, setMode] = useState<Mode>('image')
  const [imagePath, setImagePath] = useState('')
  const [imageText, setImageText] = useState('')
  const [text, setText] = useState('')
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<DishTemplateAiResult | null>(null)

  const pickImage = async () => {
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })
      const path = res.tempFilePaths?.[0]
      if (!path) return
      setImagePath(path)
      setResult(null)
    } catch {
      // 用户取消选择时不提示
    }
  }

  const switchMode = (next: Mode) => {
    if (mode === next || loading) return
    setMode(next)
    setResult(null)
  }

  const validate = (): string | null => {
    if (mode === 'image' && !imagePath) {
      return '请先拍摄或选择菜品图片'
    }
    if (mode === 'text' && !text.trim()) {
      return '请先输入菜品做法'
    }
    if (mode === 'link' && !/^https?:\/\//i.test(url.trim())) {
      return '请输入以 http/https 开头的网页链接'
    }
    return null
  }

  const run = async () => {
    if (loading) return
    const error = validate()
    if (error) {
      Taro.showToast({ title: error, icon: 'none' })
      return
    }
    setLoading(true)
    setResult(null)
    try {
      let next: DishTemplateAiResult
      if (mode === 'image') {
        next = await extractDishTemplateFromImage(imagePath, imageText)
      } else if (mode === 'text') {
        next = await extractDishTemplateFromText({ text: text.trim() })
      } else {
        next = await extractDishTemplateFromText({ url: url.trim() })
      }
      setResult(next)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'AI 识别失败，请稍后重试'
      Taro.showToast({ title: message || 'AI 识别失败，请稍后重试', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  const openConfirm = () => {
    if (!result) return
    saveDishAiDraft({
      sourceType: result.sourceType,
      sourceUrl: result.sourceUrl,
      coverImage: result.coverImage,
      name: result.dish.name,
      description: result.dish.description,
      servings: result.dish.servings,
      difficulty: result.dish.difficulty,
      tags: result.dish.tags,
      ingredients: result.dish.ingredients,
      steps: result.dish.steps,
      overallConfidence: result.overallConfidence,
      uncertainFields: result.uncertainFields,
      reviewHints: result.reviewHints,
      degraded: result.degraded,
      rejectReason: result.rejectReason,
      createdAt: Date.now()
    })
    Taro.redirectTo({ url: '/pages/dish/edit?from=ai' })
  }

  const openManual = () => {
    Taro.redirectTo({ url: '/pages/dish/edit' })
  }

  const ingredientCount = result?.dish.ingredients.length ?? 0
  const stepCount = result?.dish.steps.length ?? 0

  return (
    <View className='dai-page'>
      <View className='dai-hero'>
        <View className='dai-hero__icon'>
          <MiniIcon name='spark' size='lg' tone='bare' />
        </View>
        <View className='dai-hero__copy'>
          <Text className='dai-hero__title'>AI 帮我建菜</Text>
          <Text className='dai-hero__desc'>拍照、贴文字或链接，自动整理成菜品草稿</Text>
        </View>
      </View>

      <View className='dai-modes'>
        {MODES.map((item) => (
          <Text
            key={item.key}
            className={`dai-modes__item ${mode === item.key ? 'dai-modes__item--on' : ''}`}
            onClick={() => switchMode(item.key)}
          >
            {item.label}
          </Text>
        ))}
      </View>

      {mode === 'image' ? (
        <View className='dai-panel'>
          <View
            className={`dai-upload ${imagePath ? 'dai-upload--filled' : ''}`}
            onClick={() => void pickImage()}
          >
            {imagePath ? (
              <Image className='dai-upload__img' src={imagePath} mode='aspectFill' />
            ) : (
              <View className='dai-upload__empty'>
                <MiniIcon name='plus' size='lg' tone='primary' />
                <Text className='dai-upload__title'>拍摄或选择菜品照片</Text>
                <Text className='dai-upload__tip'>光线充足、主体清晰，识别更准确</Text>
              </View>
            )}
            {imagePath ? <Text className='dai-upload__change'>重新选择</Text> : null}
          </View>
          <Text className='dai-label'>补充说明（选填）</Text>
          <Textarea
            className='dai-textarea dai-textarea--short'
            value={imageText}
            maxlength={200}
            placeholder='如：家常做法、两人份、少辣'
            onInput={(e) => setImageText(e.detail.value)}
          />
        </View>
      ) : null}

      {mode === 'text' ? (
        <View className='dai-panel'>
          <Textarea
            className='dai-textarea'
            value={text}
            maxlength={4000}
            placeholder='粘贴菜谱或口述做法，例如：番茄炒蛋，番茄两个、鸡蛋三个，先把鸡蛋炒熟盛出，再炒番茄…'
            onInput={(e) => setText(e.detail.value)}
          />
          <Text className='dai-hint'>已输入 {text.length}/4000 字</Text>
        </View>
      ) : null}

      {mode === 'link' ? (
        <View className='dai-panel'>
          <View className='dai-link'>
            <MiniIcon name='link' size='sm' tone='muted' />
            <Input
              className='dai-link__input'
              value={url}
              placeholder='https://…'
              onInput={(e) => setUrl(e.detail.value)}
            />
          </View>
          <Text className='dai-hint'>支持菜谱网页；只整理第一道菜，广告与评论会被忽略</Text>
        </View>
      ) : null}

      <View
        className={`dai-action ${loading ? 'dai-action--loading' : ''}`}
        onClick={() => void run()}
      >
        <Text className='dai-action__text'>{loading ? 'AI 正在整理…' : '开始识别'}</Text>
      </View>

      {loading ? <Loading text='AI 正在整理菜品，图片识别通常需要 5~15 秒' /> : null}

      {result ? (
        <View className='dai-result'>
          <View className='dai-result__head'>
            <Text className='dai-result__title'>{result.dish.name || '未识别到菜名'}</Text>
            <Text
              className={`dai-result__badge dai-result__badge--${result.overallConfidence.toLowerCase()}`}
            >
              置信度{CONFIDENCE_LABEL[result.overallConfidence]}
            </Text>
          </View>

          {result.rejectReason ? (
            <Text className='dai-result__reject'>{result.rejectReason}</Text>
          ) : null}
          {result.dish.description ? (
            <Text className='dai-result__desc'>{result.dish.description}</Text>
          ) : null}

          <View className='dai-result__meta'>
            {ingredientCount > 0 ? (
              <Text className='dai-result__meta-item'>原材料 {ingredientCount} 样</Text>
            ) : null}
            {stepCount > 0 ? <Text className='dai-result__meta-item'>{stepCount} 个步骤</Text> : null}
            <Text className='dai-result__meta-item'>
              {formatDuration(result.dish.estimatedTotalSeconds)}
            </Text>
            {result.dish.servings ? (
              <Text className='dai-result__meta-item'>{result.dish.servings} 人份</Text>
            ) : null}
          </View>

          {result.uncertainFields.length > 0 ? (
            <View className='dai-result__block'>
              <Text className='dai-result__block-title'>需要重点核对</Text>
              <View className='dai-result__chips'>
                {result.uncertainFields.map((field) => (
                  <Text key={field.path} className='dai-result__chip'>
                    {field.label}
                  </Text>
                ))}
              </View>
            </View>
          ) : null}

          {result.reviewHints.length > 0 ? (
            <View className='dai-result__block'>
              <Text className='dai-result__block-title'>核对提示</Text>
              {result.reviewHints.map((hint, index) => (
                <Text key={`hint-${index}`} className='dai-result__hint'>
                  · {hint}
                </Text>
              ))}
            </View>
          ) : null}

          <View className='dai-result__actions'>
            <Text className='dai-btn dai-btn--main' onClick={openConfirm}>
              去确认并编辑
            </Text>
            <Text className='dai-btn dai-btn--ghost' onClick={() => setResult(null)}>
              重新识别
            </Text>
          </View>
          <Text className='dai-result__note'>
            AI 结果只生成草稿，保存前可逐项修改，不会自动发布
          </Text>
        </View>
      ) : null}

      <Text className='dai-manual' onClick={openManual}>
        不想用 AI？直接手动填写 →
      </Text>
    </View>
  )
}
