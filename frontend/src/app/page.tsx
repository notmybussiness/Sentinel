'use client'

import React from 'react'
import { motion } from 'framer-motion'
import { DollarSign, TrendingUp, Users, Activity, BarChart3, Shield } from 'lucide-react'
import { PortfolioCard } from '@/components/dashboard/PortfolioCard'
import { StockPriceCard } from '@/components/dashboard/StockPriceCard'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

const portfolioData = [
  {
    title: "Total Portfolio Value",
    value: "₩125,432,000",
    change: "+₩12,543",
    changePercent: "+12.4%",
    isPositive: true,
    icon: <DollarSign className="h-4 w-4" />
  },
  {
    title: "Today's Gain/Loss",
    value: "₩3,245,000",
    change: "+₩1,234",
    changePercent: "+2.7%",
    isPositive: true,
    icon: <TrendingUp className="h-4 w-4" />
  },
  {
    title: "Total Return",
    value: "₩45,230,000",
    change: "+₩5,670",
    changePercent: "+56.2%",
    isPositive: true,
    icon: <BarChart3 className="h-4 w-4" />
  },
  {
    title: "Risk Score",
    value: "7.2/10",
    change: "-0.3",
    changePercent: "-4.0%",
    isPositive: false,
    icon: <Shield className="h-4 w-4" />
  }
]

const stockData = [
  {
    symbol: "AAPL",
    name: "Apple Inc.",
    price: 175.43,
    change: 2.34,
    changePercent: 1.35,
    marketCap: "$2.7T",
    volume: "45.2M"
  },
  {
    symbol: "GOOGL",
    name: "Alphabet Inc.",
    price: 142.56,
    change: -1.23,
    changePercent: -0.86,
    marketCap: "$1.8T",
    volume: "23.1M"
  },
  {
    symbol: "MSFT",
    name: "Microsoft Corp.",
    price: 378.85,
    change: 5.67,
    changePercent: 1.52,
    marketCap: "$2.8T",
    volume: "31.4M"
  },
  {
    symbol: "TSLA",
    name: "Tesla Inc.",
    price: 248.42,
    change: -3.21,
    changePercent: -1.27,
    marketCap: "$788B",
    volume: "67.8M"
  }
]

export default function Home() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800">
      {/* Header */}
      <motion.header 
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm border-b sticky top-0 z-50"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-4">
              <div className="h-8 w-8 bg-gradient-to-r from-blue-600 to-purple-600 rounded-lg flex items-center justify-center">
                <Activity className="h-5 w-5 text-white" />
              </div>
              <div>
                <h1 className="text-xl font-bold">Project Sentinel</h1>
                <p className="text-sm text-muted-foreground">데이터 기반 투자 대시보드</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <Button variant="outline">로그인</Button>
              <Button>카카오 로그인</Button>
            </div>
          </div>
        </div>
      </motion.header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Hero Section */}
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="text-center mb-12"
        >
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            감정이 아닌 <span className="bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">데이터</span>에 기반한
          </h2>
          <p className="text-xl text-muted-foreground mb-8">
            합리적인 투자 결정을 도와주는 인텔리전트 대시보드
          </p>
          <div className="flex justify-center space-x-4">
            <Button size="lg">대시보드 시작하기</Button>
            <Button size="lg" variant="outline">데모 보기</Button>
          </div>
        </motion.div>

        {/* Portfolio Overview */}
        <motion.section 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="mb-12"
        >
          <h3 className="text-2xl font-bold mb-6">포트폴리오 개요</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {portfolioData.map((item, index) => (
              <motion.div
                key={item.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.4 + index * 0.1 }}
              >
                <PortfolioCard {...item} />
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Stock Market */}
        <motion.section 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.5 }}
          className="mb-12"
        >
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-2xl font-bold">시장 현황</h3>
            <Button variant="outline">전체 보기</Button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {stockData.map((stock, index) => (
              <motion.div
                key={stock.symbol}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.4, delay: 0.6 + index * 0.1 }}
              >
                <StockPriceCard stock={stock} />
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Features Section */}
        <motion.section 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.7 }}
          className="mb-12"
        >
          <h3 className="text-2xl font-bold text-center mb-8">핵심 기능</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              {
                icon: <BarChart3 className="h-8 w-8 text-blue-600" />,
                title: "실시간 데이터 분석",
                description: "Alpha Vantage와 Finnhub API를 통한 실시간 시장 데이터 수집 및 분석"
              },
              {
                icon: <Shield className="h-8 w-8 text-green-600" />,
                title: "리스크 관리",
                description: "포트폴리오 리스크 분석과 다각화 전략 제안으로 안전한 투자"
              },
              {
                icon: <Activity className="h-8 w-8 text-purple-600" />,
                title: "성과 추적",
                description: "투자 성과와 벤치마크 비교를 통한 체계적인 포트폴리오 관리"
              }
            ].map((feature, index) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.8 + index * 0.1 }}
              >
                <Card className="text-center">
                  <CardHeader>
                    <div className="flex justify-center mb-4">
                      {feature.icon}
                    </div>
                    <CardTitle>{feature.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground">
                      {feature.description}
                    </p>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </motion.section>
      </main>
    </div>
  )
}