import { View, Text, Input, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchCategories } from '@/api/category'
import {
  createChefDish,
  fetchChefDishes,
  publishChefDish,
  submitChefDish,
  unpublishChefDish,
  updateChefDish,
  withdrawChefDish,
  type ChefDish
} from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'

export default function ChefDishesPage() {
  useChefWorkbench()
  const [list, setList] = useState<ChefDish[]>([])
  const [name, setName] = useState('')
  const [defaultCategoryId, setDefaultCategoryId] = useState<string>()
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editName, setEditName] = useState('')
  const [editStock, setEditStock] = useState('')

  const load = async () => {
    const [page, cats] = await Promise.all([
      fetchChefDishes(),
      fetchCategories().catch(() => [])
    ])
    setList(page.records || [])
    if (cats.length > 0) {
      setDefaultCategoryId(cats[0].id)
    }
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const create = async () => {
    if (!name.trim()) {
      Taro.showToast({ title: '请填写菜名', icon: 'none' })
      return
    }
    if (!defaultCategoryId) {
      Taro.showToast({ title: '暂无分类，请联系管理员', icon: 'none' })
      return
    }
    await createChefDish({
      name: name.trim(),
      stockType: 'UNLIMITED',
      unit: '份',
      categoryId: defaultCategoryId
    })
    setName('')
    Taro.showToast({ title: '已保存草稿', icon: 'success' })
    void load()
  }

  const startEdit = (d: ChefDish) => {
    if (d.auditStatus === 'PENDING_REVIEW') {
      Taro.showToast({ title: '审核中不可编辑，请先撤回', icon: 'none' })
      return
    }
    setEditingId(d.id)
    setEditName(d.name)
    setEditStock(d.stockType === 'LIMITED' ? String(d.stock ?? 0) : '')
  }

  const saveEdit = async (d: ChefDish) => {
    if (!editName.trim()) {
      Taro.showToast({ title: '菜名不能为空', icon: 'none' })
      return
    }
    const limited = editStock.trim() !== ''
    await updateChefDish(d.id, {
      name: editName.trim(),
      categoryId: d.categoryId || defaultCategoryId,
      stockType: limited ? 'LIMITED' : 'UNLIMITED',
      stock: limited ? Number(editStock) || 0 : undefined,
      unit: d.unit || '份'
    })
    setEditingId(null)
    Taro.showToast({ title: '已保存', icon: 'success' })
    void load()
  }

  return (
    <View style={{ padding: '32px' }}>
      <Text>上传菜品后需提交审核，通过才能上架。改菜名等关键信息会重新审核；仅改份数可不审。不填写价格。</Text>
      <Input value={name} placeholder='新菜名' onInput={(e) => setName(e.detail.value)} />
      <Button type='primary' onClick={() => void create()}>
        新增草稿
      </Button>
      {list.map((d) => (
        <View key={d.id} style={{ background: '#fff', padding: '24px', marginTop: '16px', borderRadius: '16px' }}>
          {editingId === d.id ? (
            <View>
              <Input value={editName} onInput={(e) => setEditName(e.detail.value)} placeholder='菜名' />
              <Input
                value={editStock}
                type='number'
                placeholder='限量份数（空=不限）'
                onInput={(e) => setEditStock(e.detail.value)}
              />
              <Button size='mini' onClick={() => void saveEdit(d)}>
                保存
              </Button>
              <Button size='mini' onClick={() => setEditingId(null)}>
                取消
              </Button>
            </View>
          ) : (
            <>
              <Text style={{ display: 'block', fontWeight: 600 }}>{d.name}</Text>
              <Text style={{ display: 'block', color: '#888' }}>
                {d.auditStatus || ''} / {d.status}
                {d.stockType === 'LIMITED' ? ` · ${d.stock ?? 0}${d.unit || '份'}` : ' · 不限量'}
                {d.rejectReason ? ` · 驳回：${d.rejectReason}` : ''}
              </Text>
              <Button size='mini' onClick={() => startEdit(d)}>
                编辑
              </Button>
              {d.auditStatus === 'PENDING_REVIEW' ? (
                <Button size='mini' onClick={() => void withdrawChefDish(d.id).then(load)}>
                  撤回审核
                </Button>
              ) : (
                <Button size='mini' onClick={() => void submitChefDish(d.id).then(load)}>
                  提交审核
                </Button>
              )}
              {d.auditStatus === 'APPROVED' && d.status !== 'ON_SALE' && (
                <Button size='mini' onClick={() => void publishChefDish(d.id).then(load)}>
                  上架
                </Button>
              )}
              {d.status === 'ON_SALE' && (
                <Button size='mini' onClick={() => void unpublishChefDish(d.id).then(load)}>
                  下架
                </Button>
              )}
            </>
          )}
        </View>
      ))}
    </View>
  )
}
