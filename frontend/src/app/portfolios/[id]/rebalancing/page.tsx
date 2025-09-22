'use client'

import React, { useState, useEffect } from 'react'
import { useParams } from 'next/navigation'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { RebalancingAnalysis } from '@/components/rebalancing/RebalancingAnalysis'
import { StrategySelector } from '@/components/rebalancing/StrategySelector'
import { AllocationEditor } from '@/components/rebalancing/AllocationEditor'
import { RecommendationsList } from '@/components/rebalancing/RecommendationsList'
import { BarChart3, TrendingUp, AlertTriangle, CheckCircle, Settings } from 'lucide-react'

interface RebalancingPageProps {}

export default function RebalancingPage(): React.ReactElement {
  const params = useParams()
  const portfolioId = params.id as string

  const [selectedStrategy, setSelectedStrategy] = useState<string>('THRESHOLD_BASED')
  const [targetAllocation, setTargetAllocation] = useState<Record<string, number>>({})
  const [recommendation, setRecommendation] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [analysisData, setAnalysisData] = useState<any>(null)

  // 페이지 진입 시 빠른 분석 실행
  useEffect(() => {
    if (portfolioId) {
      loadQuickAnalysis()
    }
  }, [portfolioId])

  const loadQuickAnalysis = async () => {
    try {
      setIsLoading(true)
      // 기본 목표 배분 (예시 - 실제로는 사용자 설정에서 가져와야 함)
      const defaultAllocation = {
        'AAPL': 30,
        'MSFT': 25,
        'GOOGL': 20,
        'TSLA': 15,
        'NVDA': 10
      }

      const response = await fetch(`/api/v1/portfolios/${portfolioId}/rebalancing/quick-analysis?userId=1`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          targetAllocation: defaultAllocation
        })
      })

      if (response.ok) {
        const data = await response.json()
        setAnalysisData(data)
        setTargetAllocation(defaultAllocation)
      }
    } catch (error) {
      console.error('Quick analysis failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const generateRecommendation = async () => {
    if (!targetAllocation || Object.keys(targetAllocation).length === 0) {
      alert('목표 자산 배분을 설정해주세요.')
      return
    }

    try {
      setIsLoading(true)
      const response = await fetch(`/api/v1/portfolios/${portfolioId}/rebalancing/recommendation?userId=1`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          targetAllocation,
          strategyName: selectedStrategy
        })
      })

      if (response.ok) {
        const data = await response.json()
        setRecommendation(data)
      } else {
        throw new Error('Failed to generate recommendation')
      }
    } catch (error) {
      console.error('Failed to generate recommendation:', error)
      alert('추천안 생성에 실패했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  const getRebalancingStatus = () => {
    if (!analysisData) return null

    const maxDeviation = analysisData.maxDeviation

    if (maxDeviation > 15) {
      return {
        status: 'urgent',
        color: 'destructive',
        icon: <AlertTriangle className="w-4 h-4" />,
        text: '즉시 리밸런싱 필요'
      }
    } else if (maxDeviation > 10) {
      return {
        status: 'needed',
        color: 'warning',
        icon: <TrendingUp className="w-4 h-4" />,
        text: '리밸런싱 권장'
      }
    } else if (maxDeviation > 5) {
      return {
        status: 'optional',
        color: 'secondary',
        icon: <BarChart3 className="w-4 h-4" />,
        text: '선택적 리밸런싱'
      }
    } else {
      return {
        status: 'good',
        color: 'success',
        icon: <CheckCircle className="w-4 h-4" />,
        text: '균형 잡힌 포트폴리오'
      }
    }
  }

  const rebalancingStatus = getRebalancingStatus()

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl">
      {/* 헤더 */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold mb-2">포트폴리오 리밸런싱</h1>
            <p className="text-muted-foreground">
              AI 기반 리밸런싱 전략으로 포트폴리오를 최적화하세요
            </p>
          </div>
          {rebalancingStatus && (
            <Badge variant={rebalancingStatus.color as any} className="text-sm px-3 py-1">
              {rebalancingStatus.icon}
              <span className="ml-2">{rebalancingStatus.text}</span>
            </Badge>
          )}
        </div>
      </motion.div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 메인 설정 패널 */}
        <div className="lg:col-span-2 space-y-6">
          {/* 전략 선택 */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
          >
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <Settings className="w-5 h-5 mr-2" />
                  리밸런싱 전략 선택
                </CardTitle>
              </CardHeader>
              <CardContent>
                <StrategySelector
                  selectedStrategy={selectedStrategy}
                  onStrategyChange={setSelectedStrategy}
                />
              </CardContent>
            </Card>
          </motion.div>

          {/* 목표 자산 배분 */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
          >
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <BarChart3 className="w-5 h-5 mr-2" />
                  목표 자산 배분
                </CardTitle>
              </CardHeader>
              <CardContent>
                <AllocationEditor
                  allocation={targetAllocation}
                  onAllocationChange={setTargetAllocation}
                  currentAllocation={analysisData?.currentAllocation}
                />
              </CardContent>
            </Card>
          </motion.div>

          {/* 분석 실행 버튼 */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
          >
            <div className="flex gap-3">
              <Button
                onClick={generateRecommendation}
                disabled={isLoading}
                className="flex-1"
                size="lg"
              >
                {isLoading ? '분석 중...' : '리밸런싱 추천안 생성'}
              </Button>
              <Button
                variant="outline"
                onClick={loadQuickAnalysis}
                disabled={isLoading}
              >
                빠른 분석
              </Button>
            </div>
          </motion.div>
        </div>

        {/* 사이드 패널 */}
        <div className="space-y-6">
          {/* 현재 상태 분석 */}
          {analysisData && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <RebalancingAnalysis data={analysisData} />
            </motion.div>
          )}
        </div>
      </div>

      {/* 추천안 결과 */}
      {recommendation && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mt-8"
        >
          <RecommendationsList recommendation={recommendation} />
        </motion.div>
      )}
    </div>
  )
}