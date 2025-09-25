'use client'

import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ChevronLeft, ChevronRight, TrendingUp, TrendingDown, RefreshCw } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

interface StockData {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  marketCap?: string
  volume?: string
  provider: string
}

// 빅테크 회사 이름 매핑 (사용자 요청 주식들 포함)
const stockNames: { [key: string]: string } = {
  'AAPL': 'Apple Inc.',
  'TSLA': 'Tesla Inc.',
  'AMZN': 'Amazon.com Inc.',
  'AMD': 'Advanced Micro Devices Inc.',
  'INTC': 'Intel Corp.',
  'NVDA': 'NVIDIA Corp.',
  'NFLX': 'Netflix Inc.',
  'META': 'Meta Platforms Inc.',
  'BYD': 'BYD Co Ltd.',
  'COIN': 'Coinbase Global Inc.',
  'MSTR': 'MicroStrategy Inc.'
}

// 목 데이터 (API 실패 시 사용)
const mockStockData: StockData[] = [
  {
    symbol: "AAPL",
    name: "Apple Inc.",
    price: 175.43,
    change: 2.34,
    changePercent: 1.35,
    marketCap: "$2.7T",
    volume: "45.2M",
    provider: "Mock"
  },
  {
    symbol: "TSLA",
    name: "Tesla Inc.",
    price: 242.67,
    change: 8.45,
    changePercent: 3.61,
    marketCap: "$788B",
    volume: "67.8M",
    provider: "Mock"
  }
]

export function BigTechSlider() {
  const [stockData, setStockData] = useState<StockData[]>(mockStockData)
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isAutoPlay, setIsAutoPlay] = useState(true)

  // 개별 주식 API로 실제 데이터 로딩 (Rate Limit 방지)
  const fetchRealStockData = async () => {
    setIsLoading(true)
    try {
      // 사용자가 요청한 주식 심볼들
      const symbols = ['AAPL', 'TSLA', 'AMZN', 'AMD', 'INTC', 'NVDA', 'NFLX', 'META']
      const stockPromises = symbols.map(async (symbol) => {
        try {
          const response = await fetch(`http://localhost:8081/api/v1/market/price/${symbol}`)
          if (response.ok) {
            const data = await response.json()
            return {
              ...data,
              name: stockNames[symbol] || symbol,
              marketCap: getMarketCap(symbol),
              volume: getVolume(symbol)
            }
          } else {
            console.warn(`${symbol} 데이터 로딩 실패`)
            return null
          }
        } catch (error) {
          console.error(`${symbol} API 호출 실패:`, error)
          return null
        }
      })

      const results = await Promise.all(stockPromises)
      const validStocks = results.filter(stock => stock !== null) as StockData[]

      if (validStocks.length > 0) {
        setStockData(validStocks)
        console.log(`✅ 실제 API에서 로드된 주식 수: ${validStocks.length}/${symbols.length}`)
        validStocks.forEach(stock => {
          console.log(`📊 ${stock.symbol}: $${stock.price} (${stock.changePercent > 0 ? '+' : ''}${stock.changePercent.toFixed(2)}%) via ${stock.provider}`)
        })
      } else {
        console.warn('⚠️ 모든 API 호출 실패, 목 데이터 사용')
        setStockData(mockStockData)
      }
    } catch (error) {
      console.error('❌ 주식 데이터 로딩 실패:', error)
      setStockData(mockStockData)
    } finally {
      setIsLoading(false)
    }
  }

  // 시가총액 정보 (하드코딩, 실제로는 API에서 받아야 함)
  const getMarketCap = (symbol: string): string => {
    const caps: { [key: string]: string } = {
      'AAPL': '$2.7T', 'TSLA': '$788B', 'AMZN': '$1.5T', 'AMD': '$245B',
      'INTC': '$189B', 'NVDA': '$1.2T', 'NFLX': '$187B', 'META': '$820B',
      'BYD': '$95B', 'COIN': '$45B', 'MSTR': '$35B'
    }
    return caps[symbol] || 'N/A'
  }

  const getVolume = (symbol: string): string => {
    const volumes: { [key: string]: string } = {
      'AAPL': '45.2M', 'TSLA': '67.8M', 'AMZN': '28.7M', 'AMD': '42.1M',
      'INTC': '35.6M', 'NVDA': '52.1M', 'NFLX': '15.3M', 'META': '19.3M',
      'BYD': '25.4M', 'COIN': '18.7M', 'MSTR': '12.9M'
    }
    return volumes[symbol] || 'N/A'
  }

  // 다음 슬라이드
  const nextSlide = () => {
    setCurrentIndex((prevIndex) =>
      prevIndex === stockData.length - 1 ? 0 : prevIndex + 1
    )
  }

  // 이전 슬라이드
  const prevSlide = () => {
    setCurrentIndex((prevIndex) =>
      prevIndex === 0 ? stockData.length - 1 : prevIndex - 1
    )
  }

  // 자동 슬라이드
  useEffect(() => {
    if (!isAutoPlay || stockData.length <= 1) return

    const interval = setInterval(nextSlide, 4000) // 4초마다
    return () => clearInterval(interval)
  }, [isAutoPlay, stockData.length])

  // 컴포넌트 마운트시 데이터 로딩
  useEffect(() => {
    fetchRealStockData()
  }, [])

  if (isLoading) {
    return (
      <Card className="w-full max-w-4xl mx-auto h-48 flex items-center justify-center">
        <CardContent className="flex items-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>실제 주식 데이터 로딩 중...</span>
        </CardContent>
      </Card>
    )
  }

  const currentStock = stockData[currentIndex]
  const isPositive = currentStock.changePercent >= 0

  return (
    <div className="w-full max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-2xl font-bold">실시간 주식 데이터</h2>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchRealStockData}
            disabled={isLoading}
            className="flex items-center gap-2"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            새로고침
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsAutoPlay(!isAutoPlay)}
          >
            {isAutoPlay ? '자동재생 끄기' : '자동재생 켜기'}
          </Button>
        </div>
      </div>

      <Card className="relative overflow-hidden">
        <CardContent className="p-0">
          <div className="relative h-48 flex items-center">
            <AnimatePresence mode="wait">
              <motion.div
                key={currentStock.symbol}
                initial={{ opacity: 0, x: 100 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -100 }}
                transition={{ duration: 0.5 }}
                className="w-full flex items-center justify-between p-8"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-3xl font-bold">{currentStock.symbol}</h3>
                    <span className="text-sm text-muted-foreground bg-muted px-2 py-1 rounded">
                      {currentStock.provider}
                    </span>
                  </div>
                  <p className="text-lg text-muted-foreground mb-4">{currentStock.name}</p>

                  <div className="grid grid-cols-2 gap-4 text-sm text-muted-foreground">
                    <div>
                      <span className="block">시가총액</span>
                      <span className="font-medium">{currentStock.marketCap}</span>
                    </div>
                    <div>
                      <span className="block">거래량</span>
                      <span className="font-medium">{currentStock.volume}</span>
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <div className="text-4xl font-bold mb-2">
                    ${currentStock.price.toFixed(2)}
                  </div>
                  <div className={`flex items-center gap-1 justify-end text-lg ${
                    isPositive ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {isPositive ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                    <span>{isPositive ? '+' : ''}${currentStock.change.toFixed(2)}</span>
                    <span>({isPositive ? '+' : ''}{currentStock.changePercent.toFixed(2)}%)</span>
                  </div>
                </div>
              </motion.div>
            </AnimatePresence>

            {/* 네비게이션 버튼 */}
            <Button
              variant="ghost"
              size="sm"
              className="absolute left-2 top-1/2 transform -translate-y-1/2"
              onClick={prevSlide}
            >
              <ChevronLeft className="w-5 h-5" />
            </Button>

            <Button
              variant="ghost"
              size="sm"
              className="absolute right-2 top-1/2 transform -translate-y-1/2"
              onClick={nextSlide}
            >
              <ChevronRight className="w-5 h-5" />
            </Button>
          </div>

          {/* 인디케이터 */}
          <div className="flex justify-center gap-2 pb-4">
            {stockData.map((_, index) => (
              <button
                key={index}
                className={`w-2 h-2 rounded-full transition-colors ${
                  index === currentIndex ? 'bg-primary' : 'bg-muted'
                }`}
                onClick={() => setCurrentIndex(index)}
              />
            ))}
          </div>
        </CardContent>
      </Card>

      {/* 요약 정보 */}
      <div className="mt-4 text-center text-sm text-muted-foreground">
        {stockData.length}개 주식 • 실시간 데이터 •
        성공한 API: {stockData.filter(s => s.provider !== 'Mock').length}개
      </div>
    </div>
  )
}