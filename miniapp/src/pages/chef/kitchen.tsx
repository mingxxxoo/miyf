import {Button, Image, Input, Text, Textarea, View} from '@tarojs/components'
import Taro, {useDidShow} from '@tarojs/taro'
import {useState} from 'react'
import {fetchInvite, fetchMyKitchen, type InviteVo, saveMyKitchen, uploadKitchenCover} from '@/api/kitchen'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import {useChefWorkbench} from '@/hooks/useChefWorkbench'
import {toAbsoluteResourceUrl} from '@/utils/resourceUrl'
import './chef.scss'

export default function ChefKitchenPage() {
  useChefWorkbench()
  const [name, setName] = useState('')
  const [intro, setIntro] = useState('')
  const [coverImage, setCoverImage] = useState('')
  const [coverPreview, setCoverPreview] = useState('')
  const [invite, setInvite] = useState<InviteVo | null>(null)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)

  useDidShow(() => {
    void fetchMyKitchen().then((k) => {
      if (!k) return
      setName(k.name || '')
      setIntro(k.intro || '')
      setCoverImage(k.coverImage || '')
      setCoverPreview(toAbsoluteResourceUrl(k.coverImage) || '')
    })
    void fetchInvite()
      .then(setInvite)
      .catch(() => setInvite(null))
  })

  const pickCover = async () => {
    if (uploading) return
    try {
      const picked = await Taro.chooseImage({ count: 1, sizeType: ['compressed'] })
      const path = picked.tempFilePaths?.[0]
      if (!path) return
      setUploading(true)
      const url = await uploadKitchenCover(path)
      setCoverImage(url)
      setCoverPreview(toAbsoluteResourceUrl(url) || path)
      Taro.showToast({ title: '封面已上传', icon: 'success' })
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '上传失败',
        icon: 'none'
      })
    } finally {
      setUploading(false)
    }
  }

  const save = async () => {
    if (!name.trim()) {
      Taro.showToast({ title: '请填写厨房名', icon: 'none' })
      return
    }
    if (saving) return
    setSaving(true)
    try {
      await saveMyKitchen({
        name: name.trim(),
        intro: intro.trim(),
        coverImage: coverImage || undefined
      })
      Taro.showToast({ title: '已保存', icon: 'success' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>
      <View className='chef-page__cover' onClick={() => void pickCover()}>
        {coverPreview ? (
          <Image className='chef-page__cover-img' src={coverPreview} mode='aspectFill' />
        ) : null}
        <Text className='chef-page__edit-cover'>{uploading ? '上传中…' : '更换封面'}</Text>
        <View className='chef-page__big-avatar'>
          {coverPreview ? (
            <Image className='chef-page__avatar-img' src={coverPreview} mode='aspectFill' />
          ) : (
            <Text>🍳</Text>
          )}
        </View>
      </View>

      <View className='chef-page__card'>
        <View className='chef-page__field'>
          <Text className='chef-page__label'>厨房名称</Text>
          <Input
            className='chef-page__input'
            value={name}
            placeholder='例如：老王的灶'
            onInput={(e) => setName(e.detail.value)}
          />
        </View>
        <View className='chef-page__field'>
          <Text className='chef-page__label'>简介</Text>
          <Textarea
            className='chef-page__textarea'
            value={intro}
            placeholder='风格、擅长菜系、取餐说明等'
            maxlength={200}
            onInput={(e) => setIntro(e.detail.value)}
          />
        </View>
        <Button className='ck-btn-primary chef-page__btn' loading={saving} onClick={() => void save()}>
          保存资料
        </Button>
      </View>

      {invite?.code ? (
        <View className='chef-page__invite-mini'>
          <View style={{ flex: 1 }}>
            <Text className='chef-page__invite-code'>{invite.code}</Text>
            <Text className='chef-page__oc-time' style={{ display: 'block', marginTop: '6px' }}>
              邀请码 · 可复制分享给食客
            </Text>
          </View>
          <Button
            className='chef-page__btn-xs chef-page__btn-xs--solid'
            size='mini'
            onClick={() => void Taro.setClipboardData({ data: invite.code })}
          >
            复制
          </Button>
        </View>
      ) : null}
    </View>
  )
}
