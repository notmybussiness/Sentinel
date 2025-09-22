'use client'

import React, { useState } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import {
  ShoppingCart,
  TrendingDown,
  DollarSign,
  Calendar,
  Target,
  AlertCircle,
  CheckCircle,
  ArrowRight,
  ExternalLink
} from 'lucide-react'

interface RecommendationAction {
  actionType: 'BUY' | 'SELL' | 'HOLD'
  symbol: string
  currentQuantity: number
  targetQuantity: number
  quantityChange: number
  currentPrice: number
  estimatedAmount: number
  currentWeight: number
  targetWeight: number
  deviation: number
  priority: number
}

interface RecommendationProps {
  recommendation: {
    recommendationId: string
    portfolioId: number
    strategyName: string
    rebalancingNeeded: boolean
    totalDeviationPercent: number
    currentAllocation: Record<string, number>
    targetAllocation: Record<string, number>
    deviations: Record<string, number>
    actions: RecommendationAction[]
    estimatedTransactionCost: number
    createdAt: string
    nextReviewDate: string
    strategyDetails: Record<string, any>
    priority: number
    notes: string
  }
}

export const RecommendationsList: React.FC<RecommendationProps> = ({ recommendation }) => {
  const [selectedActions, setSelectedActions] = useState<Set<string>>(new Set())

  const getActionIcon = (actionType: string) => {
    switch (actionType) {
      case 'BUY':
        return <ShoppingCart className="w-4 h-4 text-green-600" />
      case 'SELL':
        return <TrendingDown className="w-4 h-4 text-red-600" />
      case 'HOLD':
        return <CheckCircle className="w-4 h-4 text-blue-600" />
      default:
        return <AlertCircle className="w-4 h-4 text-gray-600" />
    }
  }

  const getActionColor = (actionType: string) => {
    switch (actionType) {
      case 'BUY': return 'bg-green-50 border-green-200 text-green-800'
      case 'SELL': return 'bg-red-50 border-red-200 text-red-800'
      case 'HOLD': return 'bg-blue-50 border-blue-200 text-blue-800'
      default: return 'bg-gray-50 border-gray-200 text-gray-800'
    }
  }

  const getPriorityBadge = (priority: number) => {
    if (priority <= 1) return { color: 'destructive', text: '매우 높음' }
    if (priority <= 2) return { color: 'warning', text: '높음' }
    if (priority <= 3) return { color: 'default', text: '보통' }
    if (priority <= 4) return { color: 'secondary', text: '낮음' }
    return { color: 'outline', text: '매우 낮음' }
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
      minimumFractionDigits: 0,
    }).format(amount)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  }

  const toggleActionSelection = (symbol: string) => {
    const newSelected = new Set(selectedActions)
    if (newSelected.has(symbol)) {
      newSelected.delete(symbol)
    } else {
      newSelected.add(symbol)
    }
    setSelectedActions(newSelected)
  }

  const executeSelectedActions = () => {
    if (selectedActions.size === 0) {
      alert('실행할 액션을 선택해주세요.')
      return
    }

    const selectedActionsData = recommendation.actions.filter(action =>
      selectedActions.has(action.symbol)
    )

    console.log('Executing actions:', selectedActionsData)
    alert(`${selectedActions.size}개 액션이 실행 대기열에 추가되었습니다.`)
  }

  const overallPriority = getPriorityBadge(recommendation.priority)

  return (
    <div className="space-y-6">
      {/* 추천안 헤더 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center text-xl">
              <Target className="w-6 h-6 mr-2" />
              리밸런싱 추천안
            </CardTitle>
            <div className="flex items-center space-x-2">
              <Badge variant={overallPriority.color as any}>
                {overallPriority.text}
              </Badge>
              <Badge variant={recommendation.rebalancingNeeded ? 'destructive' : 'default'}>
                {recommendation.rebalancingNeeded ? '리밸런싱 필요' : '리밸런싱 불필요'}
              </Badge>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <h4 className="font-medium text-sm text-muted-foreground">사용된 전략</h4>
              <p className="font-semibold">{recommendation.strategyName}</p>
            </div>
            <div className="space-y-2">
              <h4 className="font-medium text-sm text-muted-foreground">총 편차</h4>
              <p className="font-semibold text-red-600">
                {recommendation.totalDeviationPercent.toFixed(1)}%
              </p>
            </div>
            <div className="space-y-2">
              <h4 className="font-medium text-sm text-muted-foreground">예상 거래 비용</h4>
              <p className="font-semibold text-blue-600">
                {formatCurrency(recommendation.estimatedTransactionCost)}
              </p>
            </div>
          </div>

          <Separator className="my-4" />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div className="flex items-center space-x-2">
              <Calendar className="w-4 h-4 text-muted-foreground" />
              <span className="text-muted-foreground">생성일:</span>
              <span>{formatDate(recommendation.createdAt)}</span>
            </div>
            <div className="flex items-center space-x-2">
              <Calendar className="w-4 h-4 text-muted-foreground" />
              <span className="text-muted-foreground">다음 검토일:</span>
              <span>{formatDate(recommendation.nextReviewDate)}</span>
            </div>
          </div>

          {recommendation.notes && (
            <div className="mt-4 p-3 bg-blue-50 rounded-lg">
              <p className="text-sm text-blue-800">{recommendation.notes}</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 액션 목록 */}
      {recommendation.actions.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center">
                <ArrowRight className="w-5 h-5 mr-2" />
                추천 액션 ({recommendation.actions.length}개)
              </CardTitle>
              <div className="flex items-center space-x-2">
                <span className="text-sm text-muted-foreground">
                  {selectedActions.size}개 선택됨
                </span>
                <Button
                  onClick={executeSelectedActions}
                  disabled={selectedActions.size === 0}
                  size="sm"
                >
                  선택한 액션 실행
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recommendation.actions
                .sort((a, b) => a.priority - b.priority)
                .map((action, index) => {
                  const isSelected = selectedActions.has(action.symbol)
                  const actionPriority = getPriorityBadge(action.priority)

                  return (
                    <motion.div
                      key={action.symbol}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className={`p-4 border rounded-lg cursor-pointer transition-all ${
                        isSelected
                          ? 'border-primary bg-primary/5 shadow-sm'
                          : 'border-border hover:border-primary/50'
                      }`}
                      onClick={() => toggleActionSelection(action.symbol)}
                    >
                      <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-3">
                          {getActionIcon(action.actionType)}
                          <Badge
                            variant="outline"
                            className={`font-mono ${getActionColor(action.actionType)}`}
                          >
                            {action.symbol}
                          </Badge>
                          <Badge variant={actionPriority.color as any} className="text-xs">
                            {actionPriority.text}
                          </Badge>
                        </div>

                        <div className="flex items-center space-x-2">
                          <span className={`font-semibold ${
                            action.actionType === 'BUY' ? 'text-green-600' :
                            action.actionType === 'SELL' ? 'text-red-600' : 'text-blue-600'
                          }`}>
                            {action.actionType === 'BUY' ? '매수' :
                             action.actionType === 'SELL' ? '매도' : '보유'}
                          </span>
                          <ExternalLink className="w-4 h-4 text-muted-foreground" />
                        </div>
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                        <div>
                          <span className="text-muted-foreground">현재 수량</span>
                          <div className="font-medium">
                            {action.currentQuantity.toLocaleString()}주
                          </div>
                        </div>
                        <div>
                          <span className="text-muted-foreground">목표 수량</span>
                          <div className="font-medium">
                            {action.targetQuantity.toLocaleString()}주
                          </div>
                        </div>
                        <div>
                          <span className="text-muted-foreground">변경 수량</span>
                          <div className={`font-medium ${
                            action.quantityChange > 0 ? 'text-green-600' : 'text-red-600'
                          }`}>
                            {action.quantityChange > 0 ? '+' : ''}{action.quantityChange.toLocaleString()}주
                          </div>
                        </div>
                        <div>
                          <span className="text-muted-foreground">예상 금액</span>
                          <div className="font-medium">
                            {formatCurrency(action.estimatedAmount)}
                          </div>
                        </div>
                      </div>

                      <div className="mt-3 pt-3 border-t">
                        <div className="grid grid-cols-3 gap-4 text-xs">
                          <div>
                            <span className="text-muted-foreground">현재 비중</span>
                            <div className="font-medium">{action.currentWeight.toFixed(1)}%</div>
                          </div>
                          <div>
                            <span className="text-muted-foreground">목표 비중</span>
                            <div className="font-medium">{action.targetWeight.toFixed(1)}%</div>
                          </div>
                          <div>
                            <span className="text-muted-foreground">편차</span>
                            <div className={`font-medium ${
                              action.deviation > 0 ? 'text-green-600' : 'text-red-600'
                            }`}>
                              {action.deviation > 0 ? '+' : ''}{action.deviation.toFixed(1)}%
                            </div>
                          </div>
                        </div>
                      </div>
                    </motion.div>
                  )
                })}
            </div>
          </CardContent>
        </Card>
      )}

      {/* 전략 상세 정보 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">전략 상세 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            {Object.entries(recommendation.strategyDetails).map(([key, value]) => (
              <div key={key} className="flex justify-between">
                <span className="text-muted-foreground capitalize">
                  {key.replace(/([A-Z])/g, ' $1').toLowerCase()}:
                </span>
                <span className="font-medium">
                  {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                </span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}