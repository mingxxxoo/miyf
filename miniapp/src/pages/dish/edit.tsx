import {Image, Input, Picker, Switch, Text, Textarea, View} from '@tarojs/components'
import Taro, {useRouter} from '@tarojs/taro'
import {useEffect, useMemo, useState} from 'react'
import type {DishTemplateSavePayload} from '@/api/dishTemplate'
import {
    createDishTemplate,
    fetchDishTemplateDetail,
    publishDishTemplate,
    updateDishTemplate,
    uploadDishTemplateImage
} from '@/api/dishTemplate'
import MiniIcon from '@/components/MiniIcon'
import type {DishTemplate, DishTemplateIngredient, DishTemplateStep, DishTimerType} from '@/types'
import type {DishAiDraft} from '@/utils/dishAiDraft'
import {clearDishAiDraft, loadDishAiDraft} from '@/utils/dishAiDraft'
import {toAbsoluteResourceUrl} from '@/utils/resourceUrl'
import './edit.scss'

const MAX_TAGS = 10
const MAX_STEP_MINUTES = 1440

const DIFFICULTIES = [
  { value: 'EASY', label: '简单' },
  { value: 'MEDIUM', label: '中等' },
  { value: 'HARD', label: '困难' }
]

const TIMER_OPTIONS: { value: DishTimerType; label: string }[] = [
  { value: 'countdown', label: '倒计时' },
  { value: 'countup', label: '正计时' },
  { value: 'none', label: '不计时' }
]

interface IngredientForm {
  name: string
  amount: string
  unit: string
  processing: string
  substitute: string
  note: string
  required: boolean
}

interface StepForm {
  title: string
  description: string
  durationMinutes: string
  timerType: DishTimerType
  warning: string
  completionHint: string
  required: boolean
  canUndo: boolean
  image: string
  expanded: boolean
}

const EMPTY_INGREDIENT: IngredientForm = {
  name: '',
  amount: '',
  unit: '',
  processing: '',
  substitute: '',
  note: '',
  required: true
}

const EMPTY_STEP: StepForm = {
  title: '',
  description: '',
  durationMinutes: '',
  timerType: 'none',
  warning: '',
  completionHint: '',
  required: true,
  canUndo: true,
  image: '',
  expanded: false
}

function toIngredientForm(item: DishTemplateIngredient): IngredientForm {
  return {
    name: item.name || '',
    amount: item.amount || '',
    unit: item.unit || '',
    processing: item.processing || '',
    substitute: item.substitute || '',
    note: item.note || '',
    required: item.required !== false
  }
}

function toStepForm(step: DishTemplateStep): StepForm {
  const minutes = step.durationSeconds ? Math.round(step.durationSeconds / 60) : 0
  return {
    title: step.title || '',
    description: step.description || '',
    durationMinutes: minutes > 0 ? String(minutes) : '',
    timerType: step.timerType || 'none',
    warning: step.warning || '',
    completionHint: step.completionHint || '',
    required: step.required !== false,
    canUndo: step.canUndo !== false,
    image: step.image || '',
    expanded: Boolean(step.warning || step.completionHint || step.durationSeconds)
  }
}

function difficultyLabel(value: string): string {
  return DIFFICULTIES.find((item) => item.value === value)?.label || '简单'
}

function confidenceLabel(value?: string): string {
  if (value === 'HIGH') return '高'
  if (value === 'MEDIUM') return '中'
  return '低'
}

export default function DishTemplateEditPage() {
  const router = useRouter()
  const templateId = router.params.id || ''
  const fromAi = router.params.from === 'ai'

  const [loading, setLoading] = useState(Boolean(templateId))
  const [saving, setSaving] = useState(false)
  const [detail, setDetail] = useState<DishTemplate | null>(null)
  const [aiDraft, setAiDraft] = useState<DishAiDraft | null>(null)

  const [coverImage, setCoverImage] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [servings, setServings] = useState('2')
  const [difficulty, setDifficulty] = useState('EASY')
  const [tags, setTags] = useState<string[]>([])
  const [tagInput, setTagInput] = useState('')
  const [sourceType, setSourceType] = useState('MANUAL')
  const [sourceUrl, setSourceUrl] = useState('')
  const [showMore, setShowMore] = useState(false)
  const [ingredients, setIngredients] = useState<IngredientForm[]>([{...EMPTY_INGREDIENT}])
  const [steps, setSteps] = useState<StepForm[]>([{...EMPTY_STEP}])

  useEffect(() => {
    if (!templateId) return
    let alive = true
    const load = async () => {
      try {
        const tpl = await fetchDishTemplateDetail(templateId)
        if (!alive) return
        setDetail(tpl)
        setCoverImage(tpl.coverUrl || '')
        setName(tpl.name)
        setDescription(tpl.description || '')
        setServings(String(tpl.servings || 1))
        setDifficulty(tpl.difficulty || 'EASY')
        setTags(tpl.tags || [])
        setSourceType(tpl.sourceType || 'MANUAL')
        setSourceUrl(tpl.sourceUrl || '')
        setIngredients(
          tpl.ingredients.length > 0 ? tpl.ingredients.map(toIngredientForm) : [{...EMPTY_INGREDIENT}]
        )
        setSteps(tpl.steps.length > 0 ? tpl.steps.map(toStepForm) : [{...EMPTY_STEP}])
        if (tpl.status === 'archived') {
          Taro.showToast({ title: '已归档，仅可查看', icon: 'none' })
        }
      } catch {
        // request 层已提示
      } finally {
        if (alive) setLoading(false)
      }
    }
    void load()
    return () => {
      alive = false
    }
  }, [templateId])

  useEffect(() => {
    if (templateId || !fromAi) return
    const draft = loadDishAiDraft()
    if (!draft) {
      Taro.showToast({ title: '草稿已失效，请重新识别', icon: 'none' })
      return
    }
    setAiDraft(draft)
    setCoverImage(draft.coverImage || '')
    setName(draft.name || '')
    setDescription(draft.description || '')
    setServings(draft.servings ? String(draft.servings) : '2')
    setDifficulty(
      draft.difficulty === 'MEDIUM' || draft.difficulty === 'HARD' ? draft.difficulty : 'EASY'
    )
    setTags(draft.tags || [])
    setSourceType(draft.sourceType || 'AI_TEXT')
    setSourceUrl(draft.sourceUrl || '')
    setIngredients(
      draft.ingredients && draft.ingredients.length > 0
        ? draft.ingredients.map(toIngredientForm)
        : [{...EMPTY_INGREDIENT}]
    )
    setSteps(
      draft.steps && draft.steps.length > 0 ? draft.steps.map(toStepForm) : [{...EMPTY_STEP}]
    )
  }, [templateId, fromAi])

  const totalMinutes = useMemo(() => {
    return steps.reduce((sum, step) => {
      const minutes = Number(step.durationMinutes)
      return sum + (Number.isFinite(minutes) && minutes > 0 ? minutes : 0)
    }, 0)
  }, [steps])

  const readOnly = detail?.status === 'archived'

  const chooseCover = async () => {
    if (readOnly) return
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })
      const path = res.tempFilePaths?.[0]
      if (!path) return
      Taro.showLoading({ title: '上传中…', mask: true })
      const url = await uploadDishTemplateImage(path)
      setCoverImage(url)
    } catch (err) {
      if (err instanceof Error) {
        Taro.showToast({ title: err.message || '上传失败', icon: 'none' })
      }
    } finally {
      Taro.hideLoading()
    }
  }

  const chooseStepImage = async (index: number) => {
    if (readOnly) return
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })
      const path = res.tempFilePaths?.[0]
      if (!path) return
      Taro.showLoading({ title: '上传中…', mask: true })
      const url = await uploadDishTemplateImage(path)
      updateStep(index, { image: url })
    } catch (err) {
      if (err instanceof Error) {
        Taro.showToast({ title: err.message || '上传失败', icon: 'none' })
      }
    } finally {
      Taro.hideLoading()
    }
  }

  const updateIngredient = (index: number, patch: Partial<IngredientForm>) => {
    setIngredients((prev) => prev.map((item, i) => (i === index ? {...item, ...patch} : item)))
  }

  const updateStep = (index: number, patch: Partial<StepForm>) => {
    setSteps((prev) => prev.map((item, i) => (i === index ? {...item, ...patch} : item)))
  }

  const moveStep = (index: number, delta: number) => {
    const target = index + delta
    if (target < 0 || target >= steps.length) return
    setSteps((prev) => {
      const copy = [...prev]
      const [item] = copy.splice(index, 1)
      copy.splice(target, 0, item)
      return copy
    })
  }

  const addTag = () => {
    const value = tagInput.trim()
    if (!value) return
    if (tags.length >= MAX_TAGS) {
      Taro.showToast({ title: `最多 ${MAX_TAGS} 个标签`, icon: 'none' })
      return
    }
    if (tags.includes(value)) {
      setTagInput('')
      return
    }
    setTags((prev) => [...prev, value])
    setTagInput('')
  }

  const buildPayload = (targetStatus?: 'draft' | 'pre_created'): DishTemplateSavePayload | null => {
    if (!name.trim()) {
      Taro.showToast({ title: '请填写菜品名称', icon: 'none' })
      return null
    }
    const payloadIngredients: DishTemplateIngredient[] = []
    for (const item of ingredients) {
      if (!item.name.trim()) continue
      payloadIngredients.push({
        name: item.name.trim(),
        amount: item.amount.trim() || undefined,
        unit: item.unit.trim() || undefined,
        processing: item.processing.trim() || undefined,
        required: item.required,
        substitute: item.substitute.trim() || undefined,
        note: item.note.trim() || undefined
      })
    }
    const payloadSteps: DishTemplateStep[] = []
    for (let i = 0; i < steps.length; i++) {
      const step = steps[i]
      const hasContent =
        step.title.trim() ||
        step.description.trim() ||
        step.image ||
        step.durationMinutes ||
        step.warning.trim() ||
        step.completionHint.trim()
      if (!hasContent) continue
      if (!step.description.trim()) {
        Taro.showToast({ title: `第 ${i + 1} 步请填写操作说明`, icon: 'none' })
        return null
      }
      const minutes = Number(step.durationMinutes)
      const validMinutes = Number.isFinite(minutes) && minutes > 0 ? Math.min(minutes, MAX_STEP_MINUTES) : 0
      payloadSteps.push({
        step: payloadSteps.length + 1,
        order: payloadSteps.length + 1,
        title: step.title.trim() || undefined,
        description: step.description.trim(),
        image: step.image || undefined,
        durationSeconds: validMinutes > 0 ? Math.round(validMinutes * 60) : undefined,
        timerType: validMinutes > 0 ? step.timerType : 'none',
        warning: step.warning.trim() || undefined,
        completionHint: step.completionHint.trim() || undefined,
        required: step.required,
        canUndo: step.canUndo
      })
    }
    return {
      name: name.trim(),
      coverImage: coverImage || undefined,
      description: description.trim() || undefined,
      servings: Math.max(1, Number(servings) || 1),
      difficulty,
      tags,
      ingredients: payloadIngredients,
      steps: payloadSteps,
      sourceType,
      sourceUrl: sourceUrl.trim() || undefined,
      targetStatus
    }
  }

  const save = async (publishAfter: boolean) => {
    if (saving || readOnly) return
    if (publishAfter && aiDraft && !templateId) {
      const modal = await Taro.showModal({
        title: '确认发布？',
        content:
          aiDraft.uncertainFields && aiDraft.uncertainFields.length > 0
            ? `AI 标出了 ${aiDraft.uncertainFields.length} 处待核对内容，确认已逐项核对后再发布。`
            : '请确认已核对 AI 生成的内容，发布后将进入组织菜品库。',
        confirmText: '确认发布'
      })
      if (!modal.confirm) return
    }
    const targetStatus = templateId ? undefined : publishAfter ? undefined : 'draft'
    const payload = buildPayload(publishAfter ? undefined : targetStatus)
    if (!payload) return
    if (publishAfter && (!payload.steps || payload.steps.length === 0)) {
      Taro.showToast({ title: '发布前请至少配置一个步骤', icon: 'none' })
      return
    }
    setSaving(true)
    Taro.showLoading({ title: publishAfter ? '发布中…' : '保存中…', mask: true })
    try {
      if (templateId) {
        await updateDishTemplate(templateId, payload)
        if (publishAfter) {
          await publishDishTemplate(templateId)
        }
        Taro.showToast({ title: publishAfter ? '已发布' : '已保存', icon: 'success' })
        setTimeout(() => Taro.navigateBack(), 400)
      } else {
        const id = await createDishTemplate(payload)
        clearDishAiDraft()
        if (publishAfter) {
          await publishDishTemplate(id)
        }
        Taro.showToast({ title: publishAfter ? '已发布' : '已保存', icon: 'success' })
        setTimeout(() => {
          Taro.redirectTo({ url: `/pages/dish/template-detail?id=${id}` })
        }, 400)
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : '保存失败'
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      Taro.hideLoading()
      setSaving(false)
    }
  }

  const saveAsPreCreated = async () => {
    if (saving || readOnly) return
    const payload = buildPayload('pre_created')
    if (!payload) return
    setSaving(true)
    Taro.showLoading({ title: '保存中…', mask: true })
    try {
      if (templateId) {
        await updateDishTemplate(templateId, payload)
        Taro.showToast({ title: '已保存', icon: 'success' })
        setTimeout(() => Taro.navigateBack(), 400)
      } else {
        const id = await createDishTemplate(payload)
        clearDishAiDraft()
        Taro.showToast({ title: '已存为预创建', icon: 'success' })
        setTimeout(() => {
          Taro.redirectTo({ url: `/pages/dish/template-detail?id=${id}` })
        }, 400)
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : '保存失败'
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      Taro.hideLoading()
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <View className='tpl-edit tpl-edit--loading'>
        <Text className='tpl-edit__loading-text'>正在读取菜品…</Text>
      </View>
    )
  }

  return (
    <View className='tpl-edit'>
      <View className='tpl-edit__cover-card' onClick={() => void chooseCover()}>
        {coverImage ? (
          <Image
            className='tpl-edit__cover-img'
            src={toAbsoluteResourceUrl(coverImage) || coverImage}
            mode='aspectFill'
          />
        ) : (
          <View className='tpl-edit__cover-empty'>
            <MiniIcon name='plus' size='lg' tone='primary' />
            <Text className='tpl-edit__cover-tip'>添加封面图</Text>
          </View>
        )}
        {coverImage ? <Text className='tpl-edit__cover-change'>更换封面</Text> : null}
      </View>

      {aiDraft ? (
        <View className={`tpl-edit__ai ${aiDraft.degraded ? 'tpl-edit__ai--warn' : ''}`}>
          <View className='tpl-edit__ai-head'>
            <Text className='tpl-edit__ai-title'>
              {aiDraft.degraded ? 'AI 未识别成功，请手动补全' : 'AI 草稿，请核对后保存'}
            </Text>
            <Text className='tpl-edit__ai-badge'>
              置信度{confidenceLabel(aiDraft.overallConfidence)}
            </Text>
          </View>
          <Text className='tpl-edit__ai-desc'>
            {aiDraft.rejectReason || '保存后仍是草稿，不会自动发布'}
          </Text>
          {aiDraft.uncertainFields && aiDraft.uncertainFields.length > 0 ? (
            <View className='tpl-edit__ai-chips'>
              {aiDraft.uncertainFields.map((field) => (
                <Text key={field.path} className='tpl-edit__ai-chip'>
                  {field.label}
                </Text>
              ))}
            </View>
          ) : null}
          {aiDraft.reviewHints && aiDraft.reviewHints.length > 0 ? (
            <View className='tpl-edit__ai-hints'>
              {aiDraft.reviewHints.slice(0, 4).map((hint, index) => (
                <Text key={`ai-hint-${index}`} className='tpl-edit__ai-hint'>
                  · {hint}
                </Text>
              ))}
            </View>
          ) : null}
        </View>
      ) : null}

      <View className='tpl-edit__card'>
        <Text className='tpl-edit__label'>菜品名称</Text>
        <Input
          className='tpl-edit__input tpl-edit__input--title'
          value={name}
          disabled={readOnly}
          placeholder='例如：番茄炒蛋'
          onInput={(e) => setName(e.detail.value)}
        />

        <Text className='tpl-edit__label'>菜品介绍</Text>
        <Textarea
          className='tpl-edit__textarea'
          value={description}
          disabled={readOnly}
          maxlength={500}
          placeholder='一句话说明这道菜的口味、适合场景'
          onInput={(e) => setDescription(e.detail.value)}
        />

        <View className='tpl-edit__row'>
          <View className='tpl-edit__half'>
            <Text className='tpl-edit__label'>建议份数</Text>
            <View className='tpl-edit__stepper'>
              <Text
                className='tpl-edit__stepper-btn'
                onClick={() => setServings(String(Math.max(1, (Number(servings) || 1) - 1)))}
              >
                −
              </Text>
              <Text className='tpl-edit__stepper-num'>{servings}</Text>
              <Text
                className='tpl-edit__stepper-btn'
                onClick={() => setServings(String(Math.min(99, (Number(servings) || 1) + 1)))}
              >
                ＋
              </Text>
            </View>
          </View>
          <View className='tpl-edit__half'>
            <Text className='tpl-edit__label'>难度</Text>
            <Picker
              mode='selector'
              range={DIFFICULTIES.map((item) => item.label)}
              value={Math.max(0, DIFFICULTIES.findIndex((item) => item.value === difficulty))}
              onChange={(e) => setDifficulty(DIFFICULTIES[Number(e.detail.value)]?.value || 'EASY')}
            >
              <View className='tpl-edit__picker'>
                <Text>{difficultyLabel(difficulty)}</Text>
                <Text className='tpl-edit__picker-arrow'>▾</Text>
              </View>
            </Picker>
          </View>
        </View>

        <Text className='tpl-edit__label'>标签</Text>
        <View className='tpl-edit__tags'>
          {tags.map((tag) => (
            <Text
              key={tag}
              className='tpl-edit__tag'
              onClick={() => setTags((prev) => prev.filter((item) => item !== tag))}
            >
              {tag} ×
            </Text>
          ))}
          <Input
            className='tpl-edit__tag-input'
            value={tagInput}
            disabled={readOnly}
            placeholder={tags.length >= MAX_TAGS ? '已达上限' : '输入后点添加'}
            confirmType='done'
            onInput={(e) => setTagInput(e.detail.value)}
            onConfirm={addTag}
          />
          <Text className='tpl-edit__tag-add' onClick={addTag}>
            添加
          </Text>
        </View>

        <Text className='tpl-edit__more-toggle' onClick={() => setShowMore((prev) => !prev)}>
          {showMore ? '收起来源信息 ▴' : '补充来源信息 ▾'}
        </Text>
        {showMore ? (
          <View>
            <Text className='tpl-edit__label'>来源链接（选填）</Text>
            <Input
              className='tpl-edit__input'
              value={sourceUrl}
              disabled={readOnly}
              placeholder='https://…'
              onInput={(e) => setSourceUrl(e.detail.value)}
            />
          </View>
        ) : null}
      </View>

      <View className='tpl-edit__section'>
        <View className='tpl-edit__section-head'>
          <View>
            <Text className='tpl-edit__section-title'>原材料</Text>
            <Text className='tpl-edit__section-sub'>写清用量与处理方式，做菜时不慌</Text>
          </View>
          <Text
            className='tpl-edit__section-add'
            onClick={() => setIngredients((prev) => [...prev, {...EMPTY_INGREDIENT}])}
          >
            ＋ 加一样
          </Text>
        </View>

        {ingredients.map((item, index) => (
          <View key={`ing-${index}`} className='tpl-edit__item'>
            <View className='tpl-edit__item-head'>
              <Text className='tpl-edit__item-index'>原料 {index + 1}</Text>
              <Text
                className='tpl-edit__item-del'
                onClick={() => setIngredients((prev) => prev.filter((_, i) => i !== index))}
              >
                删除
              </Text>
            </View>
            <Input
              className='tpl-edit__input'
              value={item.name}
              disabled={readOnly}
              placeholder='名称，如：鸡蛋'
              onInput={(e) => updateIngredient(index, {name: e.detail.value})}
            />
            <View className='tpl-edit__row'>
              <Input
                className='tpl-edit__input tpl-edit__half'
                value={item.amount}
                disabled={readOnly}
                placeholder='用量，如：2'
                onInput={(e) => updateIngredient(index, {amount: e.detail.value})}
              />
              <Input
                className='tpl-edit__input tpl-edit__half'
                value={item.unit}
                disabled={readOnly}
                placeholder='单位，如：个'
                onInput={(e) => updateIngredient(index, {unit: e.detail.value})}
              />
            </View>
            <Input
              className='tpl-edit__input'
              value={item.processing}
              disabled={readOnly}
              placeholder='处理方式，如：打散、切丁'
              onInput={(e) => updateIngredient(index, {processing: e.detail.value})}
            />
            <View className='tpl-edit__row'>
              <Input
                className='tpl-edit__input tpl-edit__half'
                value={item.substitute}
                disabled={readOnly}
                placeholder='替代方案（选填）'
                onInput={(e) => updateIngredient(index, {substitute: e.detail.value})}
              />
              <View className='tpl-edit__switch-half'>
                <Text className='tpl-edit__switch-label'>必需</Text>
                <Switch
                  checked={item.required}
                  disabled={readOnly}
                  color='#F07B1F'
                  onChange={(e) => updateIngredient(index, {required: e.detail.value})}
                />
              </View>
            </View>
          </View>
        ))}
      </View>

      <View className='tpl-edit__section'>
        <View className='tpl-edit__section-head'>
          <View>
            <Text className='tpl-edit__section-title'>做菜步骤</Text>
            <Text className='tpl-edit__section-sub'>
              {steps.length} 步 · 合计 {totalMinutes > 0 ? `${totalMinutes} 分钟` : '未估时'}
            </Text>
          </View>
          <Text
            className='tpl-edit__section-add'
            onClick={() => setSteps((prev) => [...prev, {...EMPTY_STEP}])}
          >
            ＋ 加一步
          </Text>
        </View>

        {steps.map((step, index) => (
          <View key={`step-${index}`} className='tpl-edit__step'>
            <View className='tpl-edit__step-head'>
              <View className='tpl-edit__step-badge'>
                <Text className='tpl-edit__step-num'>{index + 1}</Text>
                <Text className='tpl-edit__step-label'>步骤</Text>
              </View>
              <View className='tpl-edit__step-tools'>
                <Text className='tpl-edit__tool' onClick={() => moveStep(index, -1)}>
                  上移
                </Text>
                <Text className='tpl-edit__tool' onClick={() => moveStep(index, 1)}>
                  下移
                </Text>
                <Text
                  className='tpl-edit__tool tpl-edit__tool--danger'
                  onClick={() => setSteps((prev) => prev.filter((_, i) => i !== index))}
                >
                  删除
                </Text>
              </View>
            </View>

            <Input
              className='tpl-edit__input'
              value={step.title}
              disabled={readOnly}
              placeholder='小标题（选填），如：热锅下油'
              onInput={(e) => updateStep(index, {title: e.detail.value})}
            />
            <Textarea
              className='tpl-edit__textarea'
              value={step.description}
              disabled={readOnly}
              maxlength={500}
              placeholder='这一步具体做什么、做到什么程度'
              onInput={(e) => updateStep(index, {description: e.detail.value})}
            />

            <View className='tpl-edit__row'>
              <Input
                className='tpl-edit__input tpl-edit__half'
                type='number'
                value={step.durationMinutes}
                disabled={readOnly}
                placeholder='预计分钟数'
                onInput={(e) => {
                  const minutes = Number(e.detail.value)
                  updateStep(index, {
                    durationMinutes: e.detail.value,
                    timerType:
                      minutes > 0 && step.timerType === 'none' ? 'countdown' : step.timerType
                  })
                }}
              />
              <View className='tpl-edit__timers'>
                {TIMER_OPTIONS.map((option) => (
                  <Text
                    key={option.value}
                    className={`tpl-edit__timer ${
                      step.timerType === option.value ? 'tpl-edit__timer--on' : ''
                    }`}
                    onClick={() =>
                      updateStep(index, {
                        timerType: option.value,
                        durationMinutes:
                          option.value === 'none' ? '' : step.durationMinutes || '5'
                      })
                    }
                  >
                    {option.label}
                  </Text>
                ))}
              </View>
            </View>

            <View className='tpl-edit__step-extra'>
              <Text className='tpl-edit__more-toggle' onClick={() => updateStep(index, {expanded: !step.expanded})}>
                {step.expanded ? '收起提示 ▴' : '风险 / 完成提示 ▾'}
              </Text>
              {step.expanded ? (
                <View>
                  <Input
                    className='tpl-edit__input'
                    value={step.warning}
                    disabled={readOnly}
                    placeholder='风险提示，如：小心溅油'
                    onInput={(e) => updateStep(index, {warning: e.detail.value})}
                  />
                  <Input
                    className='tpl-edit__input'
                    value={step.completionHint}
                    disabled={readOnly}
                    placeholder='完成提示，如：表面金黄即可'
                    onInput={(e) => updateStep(index, {completionHint: e.detail.value})}
                  />
                </View>
              ) : null}
            </View>

            <View className='tpl-edit__step-foot'>
              {step.image ? (
                <View className='tpl-edit__step-thumb-wrap'>
                  <Image className='tpl-edit__step-thumb' src={step.image} mode='aspectFill' />
                  <Text className='tpl-edit__step-thumb-del' onClick={() => updateStep(index, {image: ''})}>
                    ×
                  </Text>
                </View>
              ) : (
                <Text className='tpl-edit__step-img' onClick={() => void chooseStepImage(index)}>
                  ＋ 步骤图
                </Text>
              )}
            </View>
          </View>
        ))}
      </View>

      {readOnly ? (
        <View className='tpl-edit__readonly'>
          <Text>已归档的菜品仅可查看，恢复为草稿后才能编辑</Text>
        </View>
      ) : (
        <View className='tpl-edit__actions'>
          {templateId ? (
            <>
              <Text className='tpl-edit__btn tpl-edit__btn--ghost' onClick={() => void save(false)}>
                保存修改
              </Text>
              {detail?.publishable ? (
                <Text className='tpl-edit__btn tpl-edit__btn--main' onClick={() => void save(true)}>
                  发布菜品
                </Text>
              ) : null}
            </>
          ) : (
            <>
              <Text className='tpl-edit__btn tpl-edit__btn--ghost' onClick={() => void saveAsPreCreated()}>
                存为预创建
              </Text>
              <Text className='tpl-edit__btn tpl-edit__btn--main' onClick={() => void save(false)}>
                存为草稿
              </Text>
            </>
          )}
        </View>
      )}
      {!templateId ? (
        <Text className='tpl-edit__publish-link' onClick={() => void save(true)}>
          内容已完整？保存并直接发布 →
        </Text>
      ) : null}
    </View>
  )
}
