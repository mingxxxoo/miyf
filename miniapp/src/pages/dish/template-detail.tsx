import {Image, Text, View} from '@tarojs/components'
import Taro, {useDidShow, useRouter} from '@tarojs/taro'
import {useState} from 'react'
import {archiveDishTemplate, deleteDishTemplate, fetchDishTemplateDetail, publishDishTemplate} from '@/api/dishTemplate'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import type {DishTemplate, DishTemplateStatus} from '@/types'
import {formatDuration, timerTypeLabel} from '@/utils/format'
import './template-detail.scss'

const STATUS_META: Record<DishTemplateStatus, { label: string; hint: string }> = {
  draft: { label: '草稿', hint: '仅自己可见，完善后可发布' },
  pre_created: { label: '预创建', hint: '已提前备好，随时可开做' },
  published: { label: '已发布', hint: '组织成员都可以选它开做' },
  archived: { label: '已归档', hint: '不再出现在菜品库' }
}

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

export default function DishTemplateDetailPage() {
  const router = useRouter()
  const templateId = router.params.id || ''
  const [detail, setDetail] = useState<DishTemplate | null>(null)
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(false)

  const load = async () => {
    if (!templateId) {
      setLoading(false)
      return
    }
    try {
      const tpl = await fetchDishTemplateDetail(templateId)
      setDetail(tpl)
      Taro.setNavigationBarTitle({ title: tpl.name || '菜品详情' })
    } catch {
      // request 层已提示
    } finally {
      setLoading(false)
    }
  }

  useDidShow(() => {
    void load()
  })

  const runAction = async (label: string, action: () => Promise<void>) => {
    if (acting) return
    setActing(true)
    Taro.showLoading({ title: `${label}中…`, mask: true })
    try {
      await action()
      Taro.showToast({ title: `${label}成功`, icon: 'success' })
      await load()
    } catch (err) {
      const message = err instanceof Error ? err.message : `${label}失败`
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      Taro.hideLoading()
      setActing(false)
    }
  }

  const confirmAndRun = async (
    title: string,
    content: string,
    label: string,
    action: () => Promise<void>
  ) => {
    const res = await Taro.showModal({ title, content, confirmText: label, cancelText: '取消' })
    if (!res.confirm) return
    await runAction(label, action)
  }

  const openMore = () => {
    if (!detail) return
    const items: { label: string; run: () => void }[] = []
    if (detail.publishable) {
      items.push({
        label: '发布菜品',
        run: () =>
          void confirmAndRun('发布菜品', '发布后组织成员都可以选它开做', '发布', () =>
            publishDishTemplate(detail.id)
          )
      })
    }
    if (detail.archivable) {
      items.push({
        label: '归档菜品',
        run: () =>
          void confirmAndRun('归档菜品', '归档后不再出现在菜品库，已创建的做菜任务不受影响', '归档', () =>
            archiveDishTemplate(detail.id)
          )
      })
    }
    if (detail.status === 'draft' || detail.status === 'pre_created') {
      items.push({
        label: '删除菜品',
        run: () =>
          void confirmAndRun('删除菜品', '删除后不可恢复，已有做菜记录时请改为归档', '删除', async () => {
            await deleteDishTemplate(detail.id)
            Taro.navigateBack()
          })
      })
    }
    if (items.length === 0) {
      Taro.showToast({ title: '暂无可执行操作', icon: 'none' })
      return
    }
    void Taro.showActionSheet({
      itemList: items.map((item) => item.label)
    })
      .then((res) => {
        items[res.tapIndex]?.run()
      })
      .catch(() => {
        // 用户取消
      })
  }

  const startCooking = () => {
    if (!detail) return
    Taro.navigateTo({ url: `/pages/cook/submit?dishId=${detail.id}` })
  }

  if (loading) {
    return (
      <View className='tpl-detail tpl-detail--loading'>
        <Loading text='正在读取菜品…' />
      </View>
    )
  }

  if (!detail) {
    return (
      <View className='tpl-detail tpl-detail--loading'>
        <Text className='tpl-detail__missing'>菜品不存在或无权查看</Text>
      </View>
    )
  }

  const status = STATUS_META[detail.status] || STATUS_META.draft
  const editable = detail.editable !== false

  return (
    <View className='tpl-detail'>
      <View className='tpl-detail__hero'>
        {detail.coverUrl ? (
          <Image className='tpl-detail__cover' src={detail.coverUrl} mode='aspectFill' />
        ) : (
          <View className='tpl-detail__cover-empty'>
            <MiniIcon name='dish' size='lg' tone='muted' />
          </View>
        )}
        <View className='tpl-detail__hero-mask' />
        <View className='tpl-detail__hero-copy'>
          <View className='tpl-detail__badges'>
            <Text className={`tpl-detail__badge tpl-detail__badge--${detail.status}`}>{status.label}</Text>
            {detail.made ? <Text className='tpl-detail__badge tpl-detail__badge--made'>做过</Text> : null}
          </View>
          <Text className='tpl-detail__name'>{detail.name}</Text>
          {detail.description ? (
            <Text className='tpl-detail__desc'>{detail.description}</Text>
          ) : null}
        </View>
      </View>

      <View className='tpl-detail__meta'>
        <View className='tpl-detail__meta-item'>
          <Text className='tpl-detail__meta-num'>{detail.steps.length}</Text>
          <Text className='tpl-detail__meta-label'>步骤</Text>
        </View>
        <View className='tpl-detail__meta-item'>
          <Text className='tpl-detail__meta-num'>{formatDuration(detail.estimatedTotalSeconds)}</Text>
          <Text className='tpl-detail__meta-label'>预计用时</Text>
        </View>
        <View className='tpl-detail__meta-item'>
          <Text className='tpl-detail__meta-num'>{detail.servings || 1}</Text>
          <Text className='tpl-detail__meta-label'>人份</Text>
        </View>
        <View className='tpl-detail__meta-item'>
          <Text className='tpl-detail__meta-num'>{detail.usageCount}</Text>
          <Text className='tpl-detail__meta-label'>做过次数</Text>
        </View>
      </View>

      <View className='tpl-detail__hint'>
        <MiniIcon name='spark' size='sm' tone='primary' />
        <Text className='tpl-detail__hint-text'>{status.hint}</Text>
      </View>

      {detail.tags.length > 0 || detail.difficulty ? (
        <View className='tpl-detail__tags'>
          {detail.difficulty ? (
            <Text className='tpl-detail__tag tpl-detail__tag--strong'>
              {DIFFICULTY_LABEL[detail.difficulty] || detail.difficulty}
            </Text>
          ) : null}
          {detail.tags.map((tag) => (
            <Text key={tag} className='tpl-detail__tag'>
              {tag}
            </Text>
          ))}
        </View>
      ) : null}

      <View className='tpl-detail__section'>
        <View className='tpl-detail__section-head'>
          <Text className='tpl-detail__section-title'>原材料</Text>
          <Text className='tpl-detail__section-count'>{detail.ingredients.length} 样</Text>
        </View>
        {detail.ingredients.length === 0 ? (
          <Text className='tpl-detail__empty-text'>还没有填写原材料</Text>
        ) : (
          <View className='tpl-detail__materials'>
            {detail.ingredients.map((item, index) => (
              <View key={`${item.name}-${index}`} className='tpl-detail__material'>
                <View className='tpl-detail__material-main'>
                  <Text className='tpl-detail__material-name'>{item.name}</Text>
                  {item.required === false ? (
                    <Text className='tpl-detail__material-optional'>可选</Text>
                  ) : null}
                  <Text className='tpl-detail__material-amount'>
                    {[item.amount, item.unit].filter(Boolean).join(' ') || '适量'}
                  </Text>
                </View>
                {item.processing ? (
                  <Text className='tpl-detail__material-sub'>处理：{item.processing}</Text>
                ) : null}
                {item.substitute ? (
                  <Text className='tpl-detail__material-sub'>可用 {item.substitute} 代替</Text>
                ) : null}
                {item.note ? <Text className='tpl-detail__material-sub'>{item.note}</Text> : null}
              </View>
            ))}
          </View>
        )}
      </View>

      <View className='tpl-detail__section'>
        <View className='tpl-detail__section-head'>
          <Text className='tpl-detail__section-title'>做菜步骤</Text>
          <Text className='tpl-detail__section-count'>{detail.steps.length} 步</Text>
        </View>
        {detail.steps.length === 0 ? (
          <Text className='tpl-detail__empty-text'>还没有配置步骤，编辑后即可发布</Text>
        ) : (
          <View className='tpl-detail__steps'>
            {detail.steps.map((step, index) => (
              <View key={`${step.step}-${index}`} className='tpl-detail__step'>
                <View className='tpl-detail__step-rail'>
                  <Text className='tpl-detail__step-num'>{index + 1}</Text>
                  {index < detail.steps.length - 1 ? <View className='tpl-detail__step-line' /> : null}
                </View>
                <View className='tpl-detail__step-body'>
                  <View className='tpl-detail__step-head'>
                    <Text className='tpl-detail__step-title'>
                      {step.title || `第 ${index + 1} 步`}
                    </Text>
                    {step.durationSeconds ? (
                      <Text className='tpl-detail__step-duration'>
                        {Math.max(1, Math.round(step.durationSeconds / 60))} 分钟 ·{' '}
                        {timerTypeLabel(step.timerType)}
                      </Text>
                    ) : step.timerType && step.timerType !== 'none' ? (
                      <Text className='tpl-detail__step-duration'>{timerTypeLabel(step.timerType)}</Text>
                    ) : null}
                  </View>
                  <Text className='tpl-detail__step-desc'>{step.description}</Text>
                  {step.image ? (
                    <Image className='tpl-detail__step-img' src={step.image} mode='widthFix' lazyLoad />
                  ) : null}
                  {step.warning ? (
                    <View className='tpl-detail__step-warn'>
                      <Text className='tpl-detail__step-warn-text'>注意：{step.warning}</Text>
                    </View>
                  ) : null}
                  {step.completionHint ? (
                    <Text className='tpl-detail__step-hint'>完成标准：{step.completionHint}</Text>
                  ) : null}
                  {step.required === false ? (
                    <Text className='tpl-detail__step-optional'>可选步骤</Text>
                  ) : null}
                </View>
              </View>
            ))}
          </View>
        )}
      </View>

      <View className='tpl-detail__section'>
        <Text className='tpl-detail__section-title'>菜品信息</Text>
        <View className='tpl-detail__info'>
          <View className='tpl-detail__info-row'>
            <Text className='tpl-detail__info-label'>创建人</Text>
            <Text className='tpl-detail__info-value'>{detail.creatorName || '组织成员'}</Text>
          </View>
          <View className='tpl-detail__info-row'>
            <Text className='tpl-detail__info-label'>更新时间</Text>
            <Text className='tpl-detail__info-value'>
              {(detail.lastModifyTime || detail.createTime || '').replace('T', ' ').slice(0, 16) || '—'}
            </Text>
          </View>
          {detail.sourceUrl ? (
            <View className='tpl-detail__info-row'>
              <Text className='tpl-detail__info-label'>来源</Text>
              <Text className='tpl-detail__info-value tpl-detail__info-link'>{detail.sourceUrl}</Text>
            </View>
          ) : null}
        </View>
      </View>

      <View className='tpl-detail__actions'>
        {editable ? (
          <Text
            className='tpl-detail__btn tpl-detail__btn--ghost'
            onClick={() => Taro.navigateTo({ url: `/pages/dish/edit?id=${detail.id}` })}
          >
            编辑菜品
          </Text>
        ) : null}
        <Text className='tpl-detail__btn tpl-detail__btn--more' onClick={openMore}>
          更多
        </Text>
        <Text className='tpl-detail__btn tpl-detail__btn--main' onClick={startCooking}>
          用它开做
        </Text>
      </View>
    </View>
  )
}
