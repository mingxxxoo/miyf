import { View, Text, Input, Button, Switch } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import {
  createChefCategory,
  deleteChefCategory,
  fetchChefCategories,
  updateChefCategory,
  type ChefCategory
} from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

export default function ChefCategoriesPage() {
  useChefWorkbench()
  const [list, setList] = useState<ChefCategory[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [enabled, setEnabled] = useState(true)
  const [saving, setSaving] = useState(false)

  const resetForm = () => {
    setEditingId(null)
    setName('')
    setEnabled(true)
  }

  const load = async () => {
    const cats = await fetchChefCategories()
    setList(cats)
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const startEdit = (c: ChefCategory) => {
    setEditingId(c.id)
    setName(c.name || '')
    setEnabled(c.status !== 'DISABLED')
  }

  const save = async () => {
    if (saving) return
    if (!name.trim()) {
      Taro.showToast({ title: '请填写分类名', icon: 'none' })
      return
    }
    setSaving(true)
    try {
      const payload = {
        name: name.trim(),
        status: enabled ? 'ENABLED' : 'DISABLED'
      }
      if (editingId) {
        await updateChefCategory(editingId, payload)
        Taro.showToast({ title: '已更新', icon: 'success' })
      } else {
        await createChefCategory(payload)
        Taro.showToast({ title: '已创建', icon: 'success' })
      }
      resetForm()
      await load()
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '保存失败',
        icon: 'none'
      })
    } finally {
      setSaving(false)
    }
  }

  const remove = async (c: ChefCategory) => {
    const res = await Taro.showModal({
      title: '删除分类',
      content: `确定删除「${c.name}」？分类下仍有菜品时无法删除。`
    })
    if (!res.confirm) return
    try {
      await deleteChefCategory(c.id)
      Taro.showToast({ title: '已删除', icon: 'success' })
      if (editingId === c.id) resetForm()
      await load()
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
        <Text className='chef-page__title'>分类管理</Text>
        <Text className='chef-page__sub'>为本厨房维护菜品分类，食客浏览时按此展示</Text>
      </View>

      <View className='chef-page__card'>
        <View className='chef-page__field'>
          <Text className='chef-page__label'>{editingId ? '编辑分类' : '新建分类'}</Text>
          <Input
            className='chef-page__input'
            value={name}
            placeholder='例如：家常菜'
            onInput={(e) => setName(e.detail.value)}
          />
        </View>
        <View className='chef-page__switch-row'>
          <View>
            <Text className='chef-page__switch-text'>启用</Text>
            <Text className='chef-page__switch-hint'>停用后新建菜品不可选</Text>
          </View>
          <Switch checked={enabled} onChange={(e) => setEnabled(Boolean(e.detail.value))} />
        </View>
        <Button className='chef-page__btn' type='primary' loading={saving} onClick={() => void save()}>
          {editingId ? '保存修改' : '创建分类'}
        </Button>
        {editingId ? (
          <Button className='chef-page__btn-ghost' onClick={resetForm}>
            取消编辑
          </Button>
        ) : null}
      </View>

      <Text className='chef-page__section-title'>我的分类</Text>
      {list.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='还没有分类' description='先建几个分类，再建菜会更顺手' />
        </View>
      ) : (
        list.map((c) => (
          <View key={c.id} className='chef-page__card'>
            <View className='chef-page__row'>
              <Text className='chef-dish__name'>{c.name}</Text>
              <Text className='chef-dish__desc'>{c.status === 'DISABLED' ? '已停用' : '启用中'}</Text>
            </View>
            <View className='chef-page__actions'>
              <Button className='chef-page__action' size='mini' onClick={() => startEdit(c)}>
                编辑
              </Button>
              <Button
                className='chef-page__action chef-page__action--danger'
                size='mini'
                onClick={() => void remove(c)}
              >
                删除
              </Button>
            </View>
          </View>
        ))
      )}
    </View>
  )
}
