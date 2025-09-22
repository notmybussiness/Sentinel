'use client'

import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Clock, Target, Zap, Info } from 'lucide-react'

interface Strategy {
  name: string
  description: string
  pros: string[]
  cons: string[]
  suitableFor: string[]
  complexity: string
}

interface StrategySelectorProps {
  selectedStrategy: string
  onStrategyChange: (strategy: string) => void
}

export const StrategySelector: React.FC<StrategySelectorProps> = ({
  selectedStrategy,
  onStrategyChange,
}) => {
  const [strategies, setStrategies] = useState<Record<string, Strategy>>({})
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    loadStrategies()
  }, [])

  const loadStrategies = async () => {
    try {
      const response = await fetch('/api/v1/portfolios/1/rebalancing/strategies')
      if (response.ok) {
        const data = await response.json()
        setStrategies(data)
      }
    } catch (error) {
      console.error('Failed to load strategies:', error)
      // 폴백 데이터
      setStrategies({
        THRESHOLD_BASED: {
          name: '임계값 기반',
          description: '자산 배분이 목표치에서 설정된 임계값(기본 5%) 이상 벗어날 때 리밸런싱을 권장합니다.',
          pros: ['시장 변동에 즉시 반응', '효율적인 리스크 관리', '거래 비용 최소화'],
          cons: ['빈번한 모니터링 필요', '변동성 시기 거래 증가'],
          suitableFor: ['적극적 투자자', '시장 반응성 중시', '큰 포트폴리오'],
          complexity: '중간'
        },
        TIME_BASED: {
          name: '시간 기반',
          description: '정해진 시간 간격(기본 3개월)에 따라 정기적으로 리밸런싱을 수행합니다.',
          pros: ['예측 가능한 일정', '감정적 결정 방지', '간단한 관리'],
          cons: ['시장 타이밍 놓칠 수 있음', '급격한 변동 대응 부족'],
          suitableFor: ['장기 투자자', '간편한 관리 선호', '안정적 성향'],
          complexity: '낮음'
        },
        HYBRID: {
          name: '하이브리드',
          description: '시간 기반과 임계값 기반 전략을 조합한 하이브리드 전략입니다.',
          pros: ['균형적 접근', '유연한 대응', '최적의 타이밍'],
          cons: ['복잡한 로직', '설정 조정 필요'],
          suitableFor: ['균형적 투자자', '맞춤형 관리 원하는 경우', '중간 규모 포트폴리오'],
          complexity: '높음'
        }
      })
    } finally {
      setIsLoading(false)
    }
  }

  const getStrategyIcon = (strategyKey: string) => {
    switch (strategyKey) {
      case 'THRESHOLD_BASED':
        return <Target className="w-5 h-5" />
      case 'TIME_BASED':
        return <Clock className="w-5 h-5" />
      case 'HYBRID':
        return <Zap className="w-5 h-5" />
      default:
        return <Info className="w-5 h-5" />
    }
  }

  const getComplexityColor = (complexity: string) => {
    switch (complexity) {
      case '낮음': return 'bg-green-100 text-green-800'
      case '중간': return 'bg-yellow-100 text-yellow-800'
      case '높음': return 'bg-red-100 text-red-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="animate-pulse">
            <div className="h-24 bg-gray-200 rounded-lg"></div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {Object.entries(strategies).map(([key, strategy], index) => (
        <motion.div
          key={key}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.1 }}
        >
          <Card
            className={`cursor-pointer transition-all duration-200 ${
              selectedStrategy === key
                ? 'ring-2 ring-primary border-primary bg-primary/5'
                : 'hover:shadow-md border-border'
            }`}
            onClick={() => onStrategyChange(key)}
          >
            <CardContent className="p-4">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-lg ${
                    selectedStrategy === key ? 'bg-primary text-primary-foreground' : 'bg-muted'
                  }`}>
                    {getStrategyIcon(key)}
                  </div>
                  <div>
                    <h3 className="font-semibold text-lg">{strategy.name}</h3>
                    <Badge className={`mt-1 ${getComplexityColor(strategy.complexity)}`}>
                      복잡도: {strategy.complexity}
                    </Badge>
                  </div>
                </div>
                <Button
                  variant={selectedStrategy === key ? 'default' : 'outline'}
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation()
                    onStrategyChange(key)
                  }}
                >
                  {selectedStrategy === key ? '선택됨' : '선택'}
                </Button>
              </div>

              <p className="text-sm text-muted-foreground mb-3">
                {strategy.description}
              </p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                <div>
                  <h4 className="font-medium text-green-700 mb-1">장점</h4>
                  <ul className="space-y-1">
                    {strategy.pros.map((pro, i) => (
                      <li key={i} className="flex items-center">
                        <div className="w-1 h-1 bg-green-500 rounded-full mr-2"></div>
                        {pro}
                      </li>
                    ))}
                  </ul>
                </div>

                <div>
                  <h4 className="font-medium text-red-700 mb-1">단점</h4>
                  <ul className="space-y-1">
                    {strategy.cons.map((con, i) => (
                      <li key={i} className="flex items-center">
                        <div className="w-1 h-1 bg-red-500 rounded-full mr-2"></div>
                        {con}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>

              <div className="mt-3 pt-3 border-t">
                <h4 className="font-medium text-blue-700 mb-1 text-xs">적합한 투자자</h4>
                <div className="flex flex-wrap gap-1">
                  {strategy.suitableFor.map((type, i) => (
                    <Badge key={i} variant="secondary" className="text-xs">
                      {type}
                    </Badge>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      ))}
    </div>
  )
}