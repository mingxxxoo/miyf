import { View, Text, Input, Textarea, Button, Image, Switch, Picker, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import { fetchCategories } from '@/api/category'
import {
  createChefDish,
  deleteChefDish,
  fetchChefDishes,
  publishChefDish,
  submitChefDish,
  unpublishChefDish,
  updateChefDish,
  uploadChefDishImage,
  withdrawChefDish,
  type ChefDish,
  type ChefDishSavePayload
} from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import type { Category } from '@/types'
import { toAbsoluteResourceUrl, toResourceUrl } from '@/utils/resourceUrl'
import './chef.scss'

const MAX_IMAGES = 6

const AUDIT_LABEL: Record<string, { text: string; tone: string }> = {
  DRAFT: { text: '草稿', tone: '' },
  PENDING_REVIEW: { text: '审核中', tone: 'warn' },
  APPROVED: { text: '已通过', tone: 'ok' },
  REJECTED: { text: '已驳回', tone: 'bad' }
}

const STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  ON_SALE: '已上架',
  OFF_SALE: '已下架'
}

function isRemotePath(path: string) {
  return path.startsWith('/r/') || /^https?:\/\//i.test(path)
}

function displaySrc(path: string) {
  return toAbsoluteResourceUrl(path) || path
}

async function resolveUploadPaths(paths: string[]): Promise<string[]> {
  const out: string[] = []
  for (const path of paths) {
    if (!path) continue
    if (isRemotePath(path)) {
      out.push(toResourceUrl(path) || path)
    } else {
      out.push(await uploadChefDishImage(path))
    }
  }
  return out
}

export default function ChefDishesPage() {
  useChefWorkbench()
  const [list, setList] = useState<ChefDish[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [name, setName] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [stockType, setStockType] = useState<'UNLIMITED' | 'LIMITED'>('UNLIMITED')
  const [stock, setStock] = useState('')
  const [unit, setUnit] = useState('份')
  const [recommend, setRecommend] = useState(false)
  const [description, setDescription] = useState('')
  const [images, setImages] = useState<string[]>([])

  const [keyword, setKeyword] = useState('')
  const [tab, setTab] = useState<'ALL' | 'ON_SALE' | 'PENDING' | 'OFF'>('ALL')
  const [creating, setCreating] = useState(false)

  const categoryNames = useMemo(() => categories.map((c) => c.name), [categories])
  const categoryIndex = Math.max(
    0,
    categories.findIndex((c) => c.id === categoryId)
  )

  const counts = useMemo(() => {
    const all = list.length
    const onSale = list.filter((d) => d.status === 'ON_SALE').length
    const pending = list.filter((d) => d.auditStatus === 'PENDING_REVIEW').length
    const off = list.filter((d) => d.status === 'OFF_SALE' || d.status === 'DRAFT').length
    return { all, onSale, pending, off }
  }, [list])

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase()
    return list.filter((d) => {
      if (tab === 'ON_SALE' && d.status !== 'ON_SALE') return false
      if (tab === 'PENDING' && d.auditStatus !== 'PENDING_REVIEW') return false
      if (tab === 'OFF' && d.status !== 'OFF_SALE' && d.status !== 'DRAFT') return false
      if (!q) return true
      return (
        (d.name || '').toLowerCase().includes(q) ||
        (d.categoryName || '').toLowerCase().includes(q)
      )
    })
  }, [list, tab, keyword])

  const showForm = creating || editingId != null || list.length === 0

  const resetForm = () => {
    setEditingId(null)
    setCreating(false)
    setName('')
    setCategoryId(categories[0]?.id || '')
    setStockType('UNLIMITED')
    setStock('')
    setUnit('份')
    setRecommend(false)
    setDescription('')
    setImages([])
  }

  const load = async () => {
    const [page, cats] = await Promise.all([
      fetchChefDishes(),
      fetchCategories().catch(() => [] as Category[])
    ])
    setList(page.records || [])
    setCategories(cats)
    if (!categoryId && cats.length > 0) {
      setCategoryId(cats[0].id)
    }
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const chooseImages = async () => {
    const remain = MAX_IMAGES - images.length
    if (remain <= 0) {
      Taro.showToast({ title: `最多 ${MAX_IMAGES} 张`, icon: 'none' })
      return
    }
    try {
      const res = await Taro.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })
      const paths = res.tempFilePaths || []
      if (paths.length) {
        setImages((prev) => [...prev, ...paths].slice(0, MAX_IMAGES))
      }
    } catch {
      // user cancel
    }
  }

  const startEdit = (d: ChefDish) => {
    if (d.auditStatus === 'PENDING_REVIEW') {
      Taro.showToast({ title: '审核中不可编辑，请先撤回', icon: 'none' })
      return
    }
    setCreating(false)
    setEditingId(d.id)
    setName(d.name || '')
    setCategoryId(d.categoryId || categories[0]?.id || '')
    setStockType(d.stockType === 'LIMITED' ? 'LIMITED' : 'UNLIMITED')
    setStock(d.stockType === 'LIMITED' ? String(d.stock ?? 0) : '')
    setUnit(d.unit || '份')
    setRecommend(Boolean(d.recommend))
    setDescription(d.description || '')
    const gallery = d.images?.length
      ? d.images
      : d.coverUrl || d.coverImage
        ? [d.coverUrl || d.coverImage!]
        : []
    setImages(gallery)
    Taro.pageScrollTo({ scrollTop: 0, duration: 200 })
  }

  const buildPayload = async (): Promise<ChefDishSavePayload | null> => {
    if (!name.trim()) {
      Taro.showToast({ title: '请填写菜名', icon: 'none' })
      return null
    }
    if (!categoryId) {
      Taro.showToast({ title: '暂无分类，请先去分类管理新建', icon: 'none' })
      return null
    }
    if (stockType === 'LIMITED' && !(Number(stock) > 0)) {
      Taro.showToast({ title: '请填写限量份数', icon: 'none' })
      return null
    }
    Taro.showLoading({ title: '保存中…', mask: true })
    try {
      const uploaded = await resolveUploadPaths(images)
      const coverImage = uploaded[0] || ''
      return {
        name: name.trim(),
        categoryId,
        stockType,
        stock: stockType === 'LIMITED' ? Number(stock) || 0 : undefined,
        unit: unit.trim() || '份',
        recommend,
        description: description.trim() || undefined,
        coverImage,
        images: uploaded
      }
    } finally {
      Taro.hideLoading()
    }
  }

  const save = async () => {
    if (saving) return
    setSaving(true)
    try {
      const payload = await buildPayload()
      if (!payload) return
      if (editingId) {
        await updateChefDish(editingId, payload)
        Taro.showToast({ title: '已保存', icon: 'success' })
      } else {
        await createChefDish(payload)
        Taro.showToast({ title: '已保存草稿', icon: 'success' })
      }
      resetForm()
      await load()
    } catch (e) {
      Taro.showToast({
        title: e instanceof Error ? e.message : '保存失败',
        icon: 'none'
      })
    } finally {
      setSaving(false)
    }
  }

  const runAction = async (fn: () => Promise<unknown>) => {
    try {
      await fn()
      await load()
    } catch {
      // request layer toasts
    }
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__search'>
        <Input
          className='chef-page__search-input'
          value={keyword}
          placeholder='搜索菜品名 / 分类'
          onInput={(e) => setKeyword(e.detail.value)}
        />
        <Text
          className='chef-page__search-add'
          onClick={() => {
            setEditingId(null)
            setCreating(true)
            setName('')
            setCategoryId(categories[0]?.id || '')
            setStockType('UNLIMITED')
            setStock('')
            setUnit('份')
            setRecommend(false)
            setDescription('')
            setImages([])
            Taro.pageScrollTo({ scrollTop: 0, duration: 200 })
          }}
        >
          ＋ 新菜品
        </Text>
      </View>

      <ScrollView scrollX className='chef-page__tabs'>
        <View className='chef-page__tabs-inner'>
          {(
            [
              { key: 'ALL', label: '全部', n: counts.all },
              { key: 'ON_SALE', label: '在售', n: counts.onSale },
              { key: 'PENDING', label: '提审中', n: counts.pending },
              { key: 'OFF', label: '已下架', n: counts.off }
            ] as const
          ).map((t) => (
            <Text
              key={t.key}
              className={`chef-page__tab ${tab === t.key ? 'chef-page__tab--on' : ''}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
              <Text className='chef-page__tab-count'>{t.n}</Text>
            </Text>
          ))}
        </View>
      </ScrollView>

      {(showForm || editingId) && (
      <View className='chef-page__card'>
        <Text className='chef-dish__form-title'>{editingId ? '编辑菜品' : '新增菜品'}</Text>

        <View className='chef-page__field'>
          <Text className='chef-page__label'>菜品图片（首张为封面）</Text>
          <View className='chef-dish__images'>
            {images.map((src, index) => (
              <View key={`${src}-${index}`} className='chef-dish__thumb-wrap'>
                <Image className='chef-dish__thumb' src={displaySrc(src)} mode='aspectFill' />
                {index === 0 && <Text className='chef-dish__thumb-cover'>封面</Text>}
                <Text
                  className='chef-dish__thumb-del'
                  onClick={() => setImages((prev) => prev.filter((_, i) => i !== index))}
                >
                  ×
                </Text>
              </View>
            ))}
            {images.length < MAX_IMAGES && (
              <View className='chef-dish__add ck-pressable' onClick={() => void chooseImages()}>
                <Text className='chef-dish__add-icon'>+</Text>
                <Text className='chef-dish__add-text'>添加</Text>
              </View>
            )}
          </View>
        </View>

        <View className='chef-page__field'>
          <Text className='chef-page__label'>菜名</Text>
          <Input
            className='chef-page__input'
            value={name}
            placeholder='例如：番茄炒蛋'
            onInput={(e) => setName(e.detail.value)}
          />
        </View>

        <View className='chef-page__field'>
          <Text className='chef-page__label'>分类</Text>
          {categories.length > 0 ? (
            <Picker
              mode='selector'
              range={categoryNames}
              value={categoryIndex}
              onChange={(e) => {
                const idx = Number(e.detail.value)
                const cat = categories[idx]
                if (cat) setCategoryId(cat.id)
              }}
            >
              <View className='chef-page__input' style={{ display: 'flex', alignItems: 'center' }}>
                <Text>{categories[categoryIndex]?.name || '请选择'}</Text>
              </View>
            </Picker>
          ) : (
            <Text
              className='ck-muted'
              onClick={() => Taro.navigateTo({ url: '/pages/chef/categories' })}
            >
              暂无分类，去新建 ›
            </Text>
          )}
        </View>

        <View className='chef-page__field'>
          <Text className='chef-page__label'>份量</Text>
          <View className='chef-page__chips'>
            <Text
              className={`chef-page__chip ${stockType === 'UNLIMITED' ? 'chef-page__chip--on' : ''}`}
              onClick={() => setStockType('UNLIMITED')}
            >
              不限量
            </Text>
            <Text
              className={`chef-page__chip ${stockType === 'LIMITED' ? 'chef-page__chip--on' : ''}`}
              onClick={() => setStockType('LIMITED')}
            >
              限量
            </Text>
          </View>
          {stockType === 'LIMITED' && (
            <Input
              className='chef-page__input'
              style={{ marginTop: '12px' }}
              type='number'
              value={stock}
              placeholder='可提供份数'
              onInput={(e) => setStock(e.detail.value)}
            />
          )}
        </View>

        <View className='chef-page__field'>
          <Text className='chef-page__label'>单位</Text>
          <Input
            className='chef-page__input'
            value={unit}
            placeholder='份 / 碗 / 盘'
            onInput={(e) => setUnit(e.detail.value)}
          />
        </View>

        <View className='chef-page__field'>
          <View className='chef-page__switch-row'>
            <View>
              <Text className='chef-page__switch-text'>标记为推荐</Text>
              <Text className='chef-page__switch-hint'>食客端推荐列表会优先展示</Text>
            </View>
            <Switch checked={recommend} color='#FFB36B' onChange={(e) => setRecommend(Boolean(e.detail.value))} />
          </View>
        </View>

        <View className='chef-page__field'>
          <Text className='chef-page__label'>简介（可选）</Text>
          <Textarea
            className='chef-page__textarea'
            value={description}
            placeholder='口味、食材或做法亮点'
            maxlength={200}
            onInput={(e) => setDescription(e.detail.value)}
          />
        </View>

        <Button className='ck-btn-primary chef-page__btn' loading={saving} onClick={() => void save()}>
          {editingId ? '保存修改' : '保存为草稿'}
        </Button>
        {editingId && (
          <Button className='chef-page__btn-ghost' onClick={resetForm}>
            取消编辑
          </Button>
        )}
      </View>
      )}

      <Text className='chef-page__section-title'>我的菜品（{filtered.length}）</Text>
      {filtered.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='没有匹配的菜品' description='换个筛选条件，或点右上角新建' />
        </View>
      ) : (
        filtered.map((d) => {
          const audit = AUDIT_LABEL[d.auditStatus || ''] || { text: d.auditStatus || '', tone: '' }
          return (
            <View key={d.id} className='chef-page__card'>
              <View className='chef-dish__item'>
                {d.coverUrl ? (
                  <Image className='chef-dish__cover' src={d.coverUrl} mode='aspectFill' />
                ) : (
                  <View className='chef-dish__cover-empty'>
                    <Text>🍳</Text>
                  </View>
                )}
                <View className='chef-dish__meta'>
                  <Text className='chef-dish__name'>{d.name}</Text>
                  <View className='chef-dish__tags'>
                    {d.recommend && <Text className='chef-dish__tag chef-dish__tag--rec'>推荐</Text>}
                    {audit.text && (
                      <Text className={`chef-dish__tag ${audit.tone ? `chef-dish__tag--${audit.tone}` : ''}`}>
                        {audit.text}
                      </Text>
                    )}
                    <Text className='chef-dish__tag'>{STATUS_LABEL[d.status] || d.status}</Text>
                  </View>
                  <Text className='chef-dish__desc'>
                    {d.categoryName ? `${d.categoryName} · ` : ''}
                    {d.stockType === 'LIMITED' ? `库存 ${d.stock ?? 0}${d.unit || '份'}` : '不限量'}
                    {d.rejectReason ? ` · 驳回：${d.rejectReason}` : ''}
                  </Text>
                </View>
              </View>
              <View className='chef-page__actions'>
                <Button className='chef-page__action chef-page__action--ghost' size='mini' onClick={() => startEdit(d)}>
                  编辑
                </Button>
                <Button
                  className='chef-page__action chef-page__action--ghost'
                  size='mini'
                  onClick={() => Taro.navigateTo({ url: `/pages/chef/recipes?dishId=${d.id}` })}
                >
                  菜谱
                </Button>
                {d.auditStatus === 'PENDING_REVIEW' && (
                  <Button
                    className='chef-page__action chef-page__action--danger'
                    size='mini'
                    onClick={() => void runAction(() => withdrawChefDish(d.id))}
                  >
                    撤回
                  </Button>
                )}
                {(d.auditStatus === 'DRAFT' || d.auditStatus === 'REJECTED' || !d.auditStatus) && (
                  <Button
                    className='chef-page__action chef-page__action--ok'
                    size='mini'
                    onClick={() =>
                      void runAction(async () => {
                        await submitChefDish(d.id)
                        Taro.showToast({ title: '已提交审核', icon: 'success' })
                      })
                    }
                  >
                    提审
                  </Button>
                )}
                {d.auditStatus === 'APPROVED' && d.status !== 'ON_SALE' && (
                  <Button
                    className='chef-page__action chef-page__action--ok'
                    size='mini'
                    onClick={() =>
                      void runAction(async () => {
                        await publishChefDish(d.id)
                        Taro.showToast({ title: '已上架', icon: 'success' })
                      })
                    }
                  >
                    上架
                  </Button>
                )}
                {d.auditStatus === 'APPROVED' && d.status === 'ON_SALE' && (
                  <Button
                    className='chef-page__action chef-page__action--danger'
                    size='mini'
                    onClick={() =>
                      void runAction(async () => {
                        await unpublishChefDish(d.id)
                        Taro.showToast({ title: '已下架', icon: 'success' })
                      })
                    }
                  >
                    下架
                  </Button>
                )}
                {(d.status === 'OFF_SALE' || d.status === 'DRAFT') &&
                  d.auditStatus !== 'PENDING_REVIEW' && (
                    <Button
                      className='chef-page__action chef-page__action--danger'
                      size='mini'
                      onClick={() =>
                        void (async () => {
                          const res = await Taro.showModal({
                            title: '删除菜品',
                            content: `确定删除「${d.name}」？删除后不可恢复。`
                          })
                          if (!res.confirm) return
                          await runAction(async () => {
                            await deleteChefDish(d.id)
                            if (editingId === d.id) resetForm()
                            Taro.showToast({ title: '已删除', icon: 'success' })
                          })
                        })()
                      }
                    >
                      删除
                    </Button>
                  )}
              </View>
            </View>
          )
        })
      )}
    </View>
  )
}
