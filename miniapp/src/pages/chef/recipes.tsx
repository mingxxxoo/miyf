import { View, Text, Input, Textarea, Button, Picker } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import {
  deleteChefRecipe,
  fetchChefDishes,
  fetchChefRecipe,
  saveChefRecipe,
  type ChefDish,
  type ChefRecipe
} from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

const DIFFS = [
  { value: 'EASY', label: '简单' },
  { value: 'MEDIUM', label: '中等' },
  { value: 'HARD', label: '较难' }
]

function linesToMaterials(text: string) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const parts = line.split(/[|｜]/)
      const name = (parts[0] || '').trim()
      const amount = (parts[1] || '').trim() || '适量'
      return { name, amount }
    })
    .filter((m) => m.name)
}

function materialsToLines(list?: { name: string; amount?: string }[]) {
  return (list || []).map((m) => `${m.name}|${m.amount || '适量'}`).join('\n')
}

function linesToSteps(text: string) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((description, idx) => ({ step: idx + 1, description }))
}

function stepsToLines(list?: { description?: string }[]) {
  return (list || []).map((s) => s.description || '').filter(Boolean).join('\n')
}

export default function ChefRecipesPage() {
  useChefWorkbench()
  const [dishes, setDishes] = useState<ChefDish[]>([])
  const [dishId, setDishId] = useState('')
  const [recipeId, setRecipeId] = useState<string | undefined>()
  const [description, setDescription] = useState('')
  const [difficulty, setDifficulty] = useState('EASY')
  const [prepareMinutes, setPrepareMinutes] = useState('')
  const [cookMinutes, setCookMinutes] = useState('')
  const [servings, setServings] = useState('2')
  const [ingredientsText, setIngredientsText] = useState('')
  const [seasoningsText, setSeasoningsText] = useState('')
  const [stepsText, setStepsText] = useState('')
  const [tips, setTips] = useState('')
  const [saving, setSaving] = useState(false)

  const dishNames = useMemo(() => dishes.map((d) => d.name), [dishes])
  const dishIndex = Math.max(
    0,
    dishes.findIndex((d) => d.id === dishId)
  )
  const diffIndex = Math.max(
    0,
    DIFFS.findIndex((d) => d.value === difficulty)
  )

  const fillRecipe = (r: ChefRecipe | null) => {
    setRecipeId(r?.id)
    setDescription(r?.description || '')
    setDifficulty(r?.difficulty || 'EASY')
    setPrepareMinutes(r?.prepareMinutes != null ? String(r.prepareMinutes) : '')
    setCookMinutes(r?.cookMinutes != null ? String(r.cookMinutes) : '')
    setServings(r?.servings != null ? String(r.servings) : '2')
    setIngredientsText(materialsToLines(r?.ingredients))
    setSeasoningsText(materialsToLines(r?.seasonings))
    setStepsText(stepsToLines(r?.steps))
    setTips(r?.tips || '')
  }

  const loadDishRecipe = async (id: string) => {
    if (!id) {
      fillRecipe(null)
      return
    }
    const recipe = await fetchChefRecipe(id)
    fillRecipe(recipe)
  }

  const load = async () => {
    const page = await fetchChefDishes()
    const records = page.records || []
    setDishes(records)
    const nextId = dishId && records.some((d) => d.id === dishId) ? dishId : records[0]?.id || ''
    setDishId(nextId)
    await loadDishRecipe(nextId)
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const save = async () => {
    if (!dishId) {
      Taro.showToast({ title: '请先创建菜品', icon: 'none' })
      return
    }
    const ingredients = linesToMaterials(ingredientsText)
    const seasonings = linesToMaterials(seasoningsText)
    const steps = linesToSteps(stepsText)
    if (!ingredients.length) {
      Taro.showToast({ title: '请填写至少一项食材', icon: 'none' })
      return
    }
    if (!steps.length) {
      Taro.showToast({ title: '请填写至少一步做法', icon: 'none' })
      return
    }
    setSaving(true)
    try {
      await saveChefRecipe(dishId, {
        description: description.trim() || undefined,
        difficulty,
        prepareMinutes: prepareMinutes ? Number(prepareMinutes) : undefined,
        cookMinutes: cookMinutes ? Number(cookMinutes) : undefined,
        servings: servings ? Number(servings) : undefined,
        tips: tips.trim() || undefined,
        ingredients,
        seasonings,
        steps
      })
      Taro.showToast({ title: '菜谱已保存', icon: 'success' })
      await loadDishRecipe(dishId)
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '保存失败',
        icon: 'none'
      })
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!dishId || !recipeId) return
    const res = await Taro.showModal({ title: '删除菜谱', content: '确定删除该菜品的菜谱？' })
    if (!res.confirm) return
    try {
      await deleteChefRecipe(dishId)
      Taro.showToast({ title: '已删除', icon: 'success' })
      fillRecipe(null)
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '删除失败',
        icon: 'none'
      })
    }
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>菜谱管理</Text>
        <Text className='chef-page__sub'>为一道菜配置食材与步骤；食材/调料按「名称|用量」分行填写</Text>
      </View>

      {dishes.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='还没有菜品' description='先去菜品管理创建菜品，再回来写菜谱' />
        </View>
      ) : (
        <>
          <View className='chef-page__card'>
            <View className='chef-page__field'>
              <Text className='chef-page__label'>选择菜品</Text>
              <Picker
                mode='selector'
                range={dishNames}
                value={dishIndex}
                onChange={(e) => {
                  const idx = Number(e.detail.value)
                  const d = dishes[idx]
                  if (!d) return
                  setDishId(d.id)
                  void loadDishRecipe(d.id)
                }}
              >
                <View className='chef-page__input' style={{ display: 'flex', alignItems: 'center' }}>
                  <Text>{dishes[dishIndex]?.name || '请选择'}</Text>
                </View>
              </Picker>
            </View>

            <View className='chef-page__field'>
              <Text className='chef-page__label'>简介</Text>
              <Textarea
                className='chef-page__textarea'
                value={description}
                placeholder='这道菜的一句话介绍'
                onInput={(e) => setDescription(e.detail.value)}
              />
            </View>

            <View className='chef-page__field'>
              <Text className='chef-page__label'>难度</Text>
              <Picker
                mode='selector'
                range={DIFFS.map((d) => d.label)}
                value={diffIndex}
                onChange={(e) => {
                  const d = DIFFS[Number(e.detail.value)]
                  if (d) setDifficulty(d.value)
                }}
              >
                <View className='chef-page__input' style={{ display: 'flex', alignItems: 'center' }}>
                  <Text>{DIFFS[diffIndex]?.label || '简单'}</Text>
                </View>
              </Picker>
            </View>

            <View className='chef-page__field'>
              <Text className='chef-page__label'>准备分钟</Text>
              <Input
                className='chef-page__input'
                type='number'
                value={prepareMinutes}
                placeholder='可选'
                onInput={(e) => setPrepareMinutes(e.detail.value)}
              />
            </View>
            <View className='chef-page__field'>
              <Text className='chef-page__label'>烹饪分钟</Text>
              <Input
                className='chef-page__input'
                type='number'
                value={cookMinutes}
                placeholder='可选'
                onInput={(e) => setCookMinutes(e.detail.value)}
              />
            </View>
            <View className='chef-page__field'>
              <Text className='chef-page__label'>建议份量</Text>
              <Input
                className='chef-page__input'
                type='number'
                value={servings}
                onInput={(e) => setServings(e.detail.value)}
              />
            </View>

            <View className='chef-page__field'>
              <Text className='chef-page__label'>食材（每行：名称|用量）</Text>
              <Textarea
                className='chef-page__textarea'
                value={ingredientsText}
                placeholder={'番茄|2个\n鸡蛋|3个'}
                onInput={(e) => setIngredientsText(e.detail.value)}
              />
            </View>
            <View className='chef-page__field'>
              <Text className='chef-page__label'>调料（每行：名称|用量）</Text>
              <Textarea
                className='chef-page__textarea'
                value={seasoningsText}
                placeholder={'盐|少许\n生抽|1勺'}
                onInput={(e) => setSeasoningsText(e.detail.value)}
              />
            </View>
            <View className='chef-page__field'>
              <Text className='chef-page__label'>步骤（每行一步）</Text>
              <Textarea
                className='chef-page__textarea'
                value={stepsText}
                placeholder={'番茄切块\n鸡蛋打散炒熟\n合炒调味'}
                onInput={(e) => setStepsText(e.detail.value)}
              />
            </View>
            <View className='chef-page__field'>
              <Text className='chef-page__label'>小贴士</Text>
              <Textarea
                className='chef-page__textarea'
                value={tips}
                placeholder='可选'
                onInput={(e) => setTips(e.detail.value)}
              />
            </View>

            <Button className='chef-page__btn' type='primary' loading={saving} onClick={() => void save()}>
              保存菜谱
            </Button>
            {recipeId ? (
              <Button className='chef-page__btn-ghost' onClick={() => void remove()}>
                删除菜谱
              </Button>
            ) : null}
          </View>
        </>
      )}
    </View>
  )
}
