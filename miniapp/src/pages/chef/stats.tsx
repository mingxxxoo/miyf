import {Text, View} from '@tarojs/components'
import Taro, {useDidShow} from '@tarojs/taro'
import {useState} from 'react'
import {type ChefKitchenStats, fetchChefStats} from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import {useChefWorkbench} from '@/hooks/useChefWorkbench'
import './chef.scss'

export default function ChefStatsPage() {
  useChefWorkbench()
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState<ChefKitchenStats | null>(null)

  const load = async () => {
    setLoading(true)
    try {
      const data = await fetchChefStats()
      setStats(data)
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '加载失败',
        icon: 'none'
      })
      setStats(null)
    } finally {
      setLoading(false)
    }
  }

  useDidShow(() => {
    void load()
  })

  const maxTrend = Math.max(1, ...(stats?.reservationTrend.map((p) => p.count) || [1]))
  const maxHot = Math.max(1, ...(stats?.hotDishes.map((h) => h.count) || [1]))

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>

      {loading && !stats ? (
        <Loading />
      ) : !stats ? (
        <EmptyState title='暂无统计' description='有预约后会显示趋势' />
      ) : (
        <>
          <View className='chef-page__sum-strip'>
            <View className='chef-page__sum-cell' style={{ background: '#FFF1E2' }}>
              <Text className='chef-page__sum-num'>{stats.todayOrders}</Text>
              <Text className='chef-page__sum-label'>今日预约</Text>
            </View>
            <View className='chef-page__sum-cell' style={{ background: '#E7F7F0' }}>
              <Text className='chef-page__sum-num'>{stats.weekOrders}</Text>
              <Text className='chef-page__sum-label'>近 7 日</Text>
            </View>
            <View className='chef-page__sum-cell' style={{ background: '#EBF3FB' }}>
              <Text className='chef-page__sum-num'>{stats.pendingOrders}</Text>
              <Text className='chef-page__sum-label'>待确认</Text>
            </View>
          </View>

          <View className='chef-page__sum-strip' style={{ marginTop: '12px' }}>
            <View className='chef-page__sum-cell' style={{ background: '#FBF3E0' }}>
              <Text className='chef-page__sum-num'>{stats.onSaleDishes}</Text>
              <Text className='chef-page__sum-label'>在售菜品</Text>
            </View>
            <View className='chef-page__sum-cell' style={{ background: '#FCEEEA' }}>
              <Text className='chef-page__sum-num'>{stats.boundDiners}</Text>
              <Text className='chef-page__sum-label'>食客</Text>
            </View>
          </View>

          <View className='chef-page__sec-row'>
            <Text className='chef-page__sec-title'>近 7 日预约</Text>
          </View>
          <View className='chef-page__card chef-page__stats-card'>
            <View className='chef-page__trend'>
              {stats.reservationTrend.map((p) => (
                <View key={p.date} className='chef-page__trend-col'>
                  <View
                    className='chef-page__trend-bar'
                    style={{
                      height: `${Math.max(8, Math.round((p.count / maxTrend) * 100))}px`
                    }}
                  />
                  <Text className='chef-page__trend-count'>{p.count}</Text>
                  <Text className='chef-page__trend-day'>{p.date}</Text>
                </View>
              ))}
            </View>
          </View>

          <View className='chef-page__sec-row'>
            <Text className='chef-page__sec-title'>热门菜品</Text>
          </View>
          {stats.hotDishes.length === 0 ? (
            <View className='chef-page__empty'>
              <EmptyState title='还没有预约数据' description='食客下单后会出现在这里' />
            </View>
          ) : (
            <View className='chef-page__card'>
              {stats.hotDishes.map((h) => (
                <View key={h.name} className='chef-page__hot-row'>
                  <Text className='chef-page__hot-name'>{h.name}</Text>
                  <View className='chef-page__hot-track'>
                    <View
                      className='chef-page__hot-fill'
                      style={{ width: `${Math.round((h.count / maxHot) * 100)}%` }}
                    />
                  </View>
                  <Text className='chef-page__hot-count'>{h.count}</Text>
                </View>
              ))}
            </View>
          )}
        </>
      )}
    </View>
  )
}
