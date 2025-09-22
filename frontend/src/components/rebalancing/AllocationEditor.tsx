'use client'

import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { Plus, Minus, RotateCcw, TrendingUp, TrendingDown } from 'lucide-react'

interface AllocationEditorProps {
  allocation: Record<string, number>
  onAllocationChange: (allocation: Record<string, number>) => void
  currentAllocation?: Record<string, number>
}

export const AllocationEditor: React.FC<AllocationEditorProps> = ({
  allocation,
  onAllocationChange,
  currentAllocation = {},
}) => {
  const [localAllocation, setLocalAllocation] = useState(allocation)
  const [totalPercentage, setTotalPercentage] = useState(0)

  useEffect(() => {
    setLocalAllocation(allocation)
  }, [allocation])

  useEffect(() => {
    const total = Object.values(localAllocation).reduce((sum, value) => sum + (value || 0), 0)
    setTotalPercentage(total)
  }, [localAllocation])

  const updateAllocation = (symbol: string, value: number) => {
    const newAllocation = {
      ...localAllocation,
      [symbol]: Math.max(0, Math.min(100, value))
    }
    setLocalAllocation(newAllocation)
    onAllocationChange(newAllocation)
  }

  const removeSymbol = (symbol: string) => {
    const newAllocation = { ...localAllocation }
    delete newAllocation[symbol]
    setLocalAllocation(newAllocation)
    onAllocationChange(newAllocation)
  }

  const addSymbol = () => {
    const newSymbol = prompt('추가할 종목 심볼을 입력하세요 (예: AAPL):')
    if (newSymbol && newSymbol.trim()) {
      const symbol = newSymbol.trim().toUpperCase()
      if (!localAllocation[symbol]) {
        updateAllocation(symbol, 0)
      }
    }
  }

  const autoBalance = () => {
    const symbols = Object.keys(localAllocation)
    if (symbols.length === 0) return

    const balancedPercentage = 100 / symbols.length
    const newAllocation = symbols.reduce((acc, symbol) => {
      acc[symbol] = Math.round(balancedPercentage * 100) / 100
      return acc
    }, {} as Record<string, number>)

    setLocalAllocation(newAllocation)
    onAllocationChange(newAllocation)
  }

  const resetToEqual = () => {
    const symbols = Object.keys(localAllocation)
    if (symbols.length === 0) return

    const equalPercentage = Math.floor(10000 / symbols.length) / 100 // 소수점 2자리까지
    const newAllocation = symbols.reduce((acc, symbol) => {
      acc[symbol] = equalPercentage
      return acc
    }, {} as Record<string, number>)

    // 나머지를 첫 번째 항목에 추가
    const remainder = 100 - (equalPercentage * symbols.length)
    if (symbols.length > 0) {
      newAllocation[symbols[0]] += remainder
    }

    setLocalAllocation(newAllocation)
    onAllocationChange(newAllocation)
  }

  const getDeviationColor = (current: number, target: number) => {
    const deviation = Math.abs(current - target)
    if (deviation > 10) return 'text-red-600'
    if (deviation > 5) return 'text-yellow-600'
    return 'text-green-600'
  }

  const getProgressColor = () => {
    if (Math.abs(totalPercentage - 100) < 1) return 'bg-green-500'
    if (totalPercentage > 100) return 'bg-red-500'
    return 'bg-blue-500'
  }

  return (
    <div className="space-y-6">
      {/* 총 배분 현황 */}
      <Card>
        <CardContent className="p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold">총 배분 현황</h3>
            <Badge
              variant={Math.abs(totalPercentage - 100) < 1 ? 'default' : 'destructive'}
            >
              {totalPercentage.toFixed(1)}%
            </Badge>
          </div>
          <Progress
            value={Math.min(totalPercentage, 100)}
            className="w-full h-3"
          />
          <div className="flex justify-between text-xs text-muted-foreground mt-2">
            <span>0%</span>
            <span className={totalPercentage === 100 ? 'text-green-600 font-medium' : ''}>
              목표: 100%
            </span>
            <span>100%</span>
          </div>
        </CardContent>
      </Card>

      {/* 액션 버튼들 */}
      <div className="flex gap-2 flex-wrap">
        <Button onClick={addSymbol} variant="outline" size="sm">
          <Plus className="w-4 h-4 mr-1" />
          종목 추가
        </Button>
        <Button onClick={autoBalance} variant="outline" size="sm">
          <RotateCcw className="w-4 h-4 mr-1" />
          자동 균등 배분
        </Button>
        <Button onClick={resetToEqual} variant="outline" size="sm">
          초기화
        </Button>
      </div>

      {/* 종목별 배분 설정 */}
      <div className="space-y-3">
        {Object.entries(localAllocation).map(([symbol, targetValue], index) => {
          const currentValue = currentAllocation[symbol] || 0
          const deviation = currentValue - targetValue

          return (
            <motion.div
              key={symbol}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.05 }}
            >
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center space-x-3">
                      <Badge variant="outline" className="font-mono">
                        {symbol}
                      </Badge>
                      {currentAllocation[symbol] !== undefined && (
                        <div className="flex items-center space-x-2">
                          <span className="text-xs text-muted-foreground">현재:</span>
                          <span className="text-sm font-medium">
                            {currentValue.toFixed(1)}%
                          </span>
                          <div className={`flex items-center text-xs ${getDeviationColor(currentValue, targetValue)}`}>
                            {deviation > 0 ? (
                              <TrendingUp className="w-3 h-3 mr-1" />
                            ) : deviation < 0 ? (
                              <TrendingDown className="w-3 h-3 mr-1" />
                            ) : null}
                            {deviation !== 0 && `${deviation > 0 ? '+' : ''}${deviation.toFixed(1)}%`}
                          </div>
                        </div>
                      )}
                    </div>
                    <Button
                      onClick={() => removeSymbol(symbol)}
                      variant="ghost"
                      size="sm"
                      className="text-red-500 hover:text-red-700"
                    >
                      <Minus className="w-4 h-4" />
                    </Button>
                  </div>

                  <div className="flex items-center space-x-4">
                    <div className="flex-1">
                      <div className="flex items-center space-x-2">
                        <Input
                          type="number"
                          value={targetValue}
                          onChange={(e) => updateAllocation(symbol, parseFloat(e.target.value) || 0)}
                          min="0"
                          max="100"
                          step="0.1"
                          className="w-20"
                        />
                        <span className="text-sm text-muted-foreground">%</span>
                      </div>
                    </div>

                    <div className="flex-1">
                      <div className="space-y-2">
                        <Progress
                          value={targetValue}
                          className="w-full h-2"
                        />
                        <div className="flex justify-between text-xs text-muted-foreground">
                          <span>목표 비중</span>
                          <span>{targetValue.toFixed(1)}%</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 현재 vs 목표 비교 (현재 데이터가 있는 경우) */}
                  {currentAllocation[symbol] !== undefined && (
                    <div className="mt-3 pt-3 border-t">
                      <div className="grid grid-cols-2 gap-4 text-xs">
                        <div>
                          <span className="text-muted-foreground">현재 비중</span>
                          <div className="font-medium">{currentValue.toFixed(1)}%</div>
                        </div>
                        <div>
                          <span className="text-muted-foreground">편차</span>
                          <div className={`font-medium ${getDeviationColor(currentValue, targetValue)}`}>
                            {deviation > 0 ? '+' : ''}{deviation.toFixed(1)}%
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </motion.div>
          )
        })}
      </div>

      {Object.keys(localAllocation).length === 0 && (
        <Card>
          <CardContent className="p-8 text-center">
            <div className="text-muted-foreground">
              <Plus className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p>아직 설정된 종목이 없습니다.</p>
              <p className="text-sm">종목 추가 버튼을 클릭하여 시작하세요.</p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* 설정 도움말 */}
      {totalPercentage !== 100 && Object.keys(localAllocation).length > 0 && (
        <Card className="border-yellow-200 bg-yellow-50">
          <CardContent className="p-4">
            <div className="flex items-center space-x-2 text-yellow-800">
              <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
              <span className="text-sm">
                총 배분이 100%가 되도록 조정해주세요.
                현재: {totalPercentage.toFixed(1)}%,
                부족/초과: {(100 - totalPercentage).toFixed(1)}%
              </span>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}