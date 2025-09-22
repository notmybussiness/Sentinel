'use client'

import React from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { BarChart3, TrendingUp, TrendingDown, AlertTriangle, CheckCircle } from 'lucide-react'

interface RebalancingAnalysisProps {
  data: {
    currentAllocation: Record<string, number>
    targetAllocation: Record<string, number>
    deviations: Record<string, number>
    maxDeviation: number
    maxDeviationSymbol: string
    needsAttention: boolean
  }
}

export const RebalancingAnalysis: React.FC<RebalancingAnalysisProps> = ({ data }) => {
  const {
    currentAllocation,
    targetAllocation,
    deviations,
    maxDeviation,
    maxDeviationSymbol,
    needsAttention
  } = data

  const getDeviationStatus = (deviation: number) => {
    const absDeviation = Math.abs(deviation)
    if (absDeviation > 15) return { color: 'destructive', level: '위험' }
    if (absDeviation > 10) return { color: 'warning', level: '주의' }
    if (absDeviation > 5) return { color: 'secondary', level: '보통' }
    return { color: 'success', level: '양호' }
  }

  const getOverallStatus = () => {
    if (maxDeviation > 15) {
      return {
        icon: <AlertTriangle className="w-5 h-5" />,
        color: 'text-red-600',
        bgColor: 'bg-red-50',
        status: '즉시 리밸런싱 필요',
        description: '큰 편차로 인해 리스크가 증가했습니다.'
      }
    } else if (maxDeviation > 10) {
      return {
        icon: <TrendingUp className="w-5 h-5" />,
        color: 'text-yellow-600',
        bgColor: 'bg-yellow-50',
        status: '리밸런싱 권장',
        description: '목표 배분에서 다소 벗어났습니다.'
      }
    } else if (maxDeviation > 5) {
      return {
        icon: <BarChart3 className="w-5 h-5" />,
        color: 'text-blue-600',
        bgColor: 'bg-blue-50',
        status: '선택적 리밸런싱',
        description: '현재 상태가 양호합니다.'
      }
    } else {
      return {
        icon: <CheckCircle className="w-5 h-5" />,
        color: 'text-green-600',
        bgColor: 'bg-green-50',
        status: '균형 잡힌 포트폴리오',
        description: '목표 배분에 잘 맞춰져 있습니다.'
      }
    }
  }

  const overallStatus = getOverallStatus()

  return (
    <div className="space-y-4">
      {/* 전체 상태 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center text-lg">
            <BarChart3 className="w-5 h-5 mr-2" />
            포트폴리오 분석
          </CardTitle>
        </CardHeader>
        <CardContent>
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`p-4 rounded-lg ${overallStatus.bgColor}`}
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-2">
                <div className={overallStatus.color}>
                  {overallStatus.icon}
                </div>
                <span className={`font-semibold ${overallStatus.color}`}>
                  {overallStatus.status}
                </span>
              </div>
              <Badge variant={needsAttention ? 'destructive' : 'default'}>
                최대 편차: {maxDeviation.toFixed(1)}%
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground">
              {overallStatus.description}
            </p>
            {maxDeviationSymbol && (
              <p className="text-xs mt-2 text-muted-foreground">
                가장 큰 편차: <span className="font-medium">{maxDeviationSymbol}</span>
              </p>
            )}
          </motion.div>
        </CardContent>
      </Card>

      {/* 종목별 편차 분석 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">종목별 편차 분석</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {Object.entries(deviations)
            .sort(([,a], [,b]) => Math.abs(b) - Math.abs(a))
            .map(([symbol, deviation], index) => {
              const current = currentAllocation[symbol] || 0
              const target = targetAllocation[symbol] || 0
              const status = getDeviationStatus(deviation)

              return (
                <motion.div
                  key={symbol}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="p-3 bg-muted/50 rounded-lg"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-3">
                      <Badge variant="outline" className="font-mono">
                        {symbol}
                      </Badge>
                      <Badge variant={status.color as any} className="text-xs">
                        {status.level}
                      </Badge>
                    </div>
                    <div className="flex items-center space-x-2 text-sm">
                      {deviation > 0 ? (
                        <TrendingUp className="w-4 h-4 text-green-600" />
                      ) : deviation < 0 ? (
                        <TrendingDown className="w-4 h-4 text-red-600" />
                      ) : null}
                      <span className={`font-medium ${
                        deviation > 0 ? 'text-green-600' :
                        deviation < 0 ? 'text-red-600' : 'text-gray-600'
                      }`}>
                        {deviation > 0 ? '+' : ''}{deviation.toFixed(1)}%
                      </span>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>현재: {current.toFixed(1)}%</span>
                      <span>목표: {target.toFixed(1)}%</span>
                    </div>

                    {/* 현재 vs 목표 시각화 */}
                    <div className="relative">
                      <Progress value={target} className="h-2 bg-gray-200" />
                      <div
                        className="absolute top-0 h-2 bg-blue-500 opacity-60 rounded-full"
                        style={{
                          width: `${Math.min(current, 100)}%`,
                          transition: 'width 0.3s ease'
                        }}
                      />
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-blue-600">■ 현재</span>
                      <span className="text-gray-600">■ 목표</span>
                    </div>
                  </div>
                </motion.div>
              )
            })}
        </CardContent>
      </Card>

      {/* 요약 통계 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">분석 요약</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-muted-foreground">추적 종목 수</span>
                <span className="font-medium">
                  {Object.keys(currentAllocation).length}개
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">최대 편차</span>
                <span className="font-medium">
                  {maxDeviation.toFixed(1)}%
                </span>
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-muted-foreground">평균 편차</span>
                <span className="font-medium">
                  {(Object.values(deviations).reduce((sum, dev) => sum + Math.abs(dev), 0) / Object.values(deviations).length).toFixed(1)}%
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">주의 필요</span>
                <span className={`font-medium ${needsAttention ? 'text-red-600' : 'text-green-600'}`}>
                  {needsAttention ? 'Yes' : 'No'}
                </span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}