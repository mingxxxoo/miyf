import { View, Text, Image } from '@tarojs/components'
import type { RecipeStep as RecipeStepType } from '@/types'
import './RecipeStep.scss'

interface RecipeStepProps {
  step: RecipeStepType
  isLast?: boolean
}

export default function RecipeStep({ step, isLast = false }: RecipeStepProps) {
  const title = step.title || `步骤 ${step.step}`
  const description = step.description || step.content || ''

  return (
    <View className={`recipe-step ${isLast ? 'recipe-step--last' : ''}`}>
      <View className='recipe-step__indicator'>
        <View className='recipe-step__num'>
          <Text>{step.step}</Text>
        </View>
        {!isLast && <View className='recipe-step__line' />}
      </View>
      <View className='recipe-step__content'>
        <Text className='recipe-step__title'>{title}</Text>
        {!!description && <Text className='recipe-step__desc'>{description}</Text>}
        {step.durationMinutes != null && (
          <Text className='recipe-step__duration'>约 {step.durationMinutes} 分钟</Text>
        )}
        {(step.imageUrl || step.image) && (
          <Image className='recipe-step__image' src={step.imageUrl || step.image!} mode='aspectFill' />
        )}
      </View>
    </View>
  )
}
