'use client'

import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { DollarSign, TrendingUp, Users, Activity, BarChart3, Shield, LogOut, Home, RefreshCw } from 'lucide-react'
import { PortfolioCard } from '@/components/dashboard/PortfolioCard'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useRouter } from 'next/navigation'

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

// 리밸런싱 추천 데이터
const rebalancingRecommendations = [
  {
    symbol: "AAPL",
    name: "Apple Inc.",
    currentWeight: 35,
    targetWeight: 25,
    action: "SELL",
    amount: "₩12,500,000",
    reason: "포트폴리오 내 비중이 과도하게 높음"
  },
  {
    symbol: "GOOGL",
    name: "Alphabet Inc.",
    currentWeight: 15,
    targetWeight: 20,
    action: "BUY",
    amount: "₩6,250,000",
    reason: "장기 성장성을 고려하여 비중 확대 권장"
  },
  {
    symbol: "MSFT",
    name: "Microsoft Corp.",
    currentWeight: 20,
    targetWeight: 25,
    action: "BUY",
    amount: "₩6,250,000",
    reason: "클라우드 사업 성장으로 추가 투자 권장"
  }
]

export default function MyPage() {
  const [user, setUser] = useState<any>(null)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const router = useRouter()

  const handleLogout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
    setIsLoggedIn(false)
    router.push('/')
  }

  const handleGoHome = () => {
    router.push('/')
  }

  // 컴포넌트 마운트 시 로그인 상태 확인
  useEffect(() => {
    const accessToken = localStorage.getItem('accessToken')
    const userData = localStorage.getItem('user')

    if (accessToken && userData) {
      setUser(JSON.parse(userData))
      setIsLoggedIn(true)
    } else {
      // 로그인하지 않은 경우 홈으로 리다이렉트
      router.push('/')
    }
  }, [router])

  if (!isLoggedIn || !user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-muted-foreground">로그인 정보를 확인하는 중...</p>
        </div>
      </div>
    )
  }

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
                <p className="text-sm text-muted-foreground">마이페이지</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <img
                  src={user.profileImageUrl}
                  alt={user.name}
                  className="h-8 w-8 rounded-full"
                />
                <span className="text-sm font-medium">{user.name}</span>
              </div>
              <Button variant="outline" size="sm" onClick={handleGoHome}>
                <Home className="h-4 w-4 mr-2" />
                홈
              </Button>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                <LogOut className="h-4 w-4 mr-2" />
                로그아웃
              </Button>
            </div>
          </div>
        </div>
      </motion.header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* User Welcome Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mb-8"
        >
          <div className="flex items-center space-x-4 mb-6">
            <img
              src={user.profileImageUrl}
              alt={user.name}
              className="h-16 w-16 rounded-full"
            />
            <div>
              <h2 className="text-3xl font-bold text-gray-900 dark:text-white">
                안녕하세요, {user.name}님!
              </h2>
              <p className="text-lg text-muted-foreground">
                포트폴리오 현황과 투자 추천을 확인하세요
              </p>
            </div>
          </div>
        </motion.div>

        {/* Portfolio Overview */}
        <motion.section
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mb-12"
        >
          <h3 className="text-2xl font-bold mb-6">내 포트폴리오</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {portfolioData.map((item, index) => (
              <motion.div
                key={item.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.3 + index * 0.1 }}
              >
                <PortfolioCard {...item} />
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Rebalancing Recommendations */}
        <motion.section
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.6 }}
          className="mb-12"
        >
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-2xl font-bold">리밸런싱 추천</h3>
            <Button variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" />
              새로고침
            </Button>
          </div>
          <div className="space-y-4">
            {rebalancingRecommendations.map((rec, index) => (
              <motion.div
                key={rec.symbol}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.4, delay: 0.7 + index * 0.1 }}
              >
                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-4">
                        <div>
                          <h4 className="font-semibold">{rec.symbol}</h4>
                          <p className="text-sm text-muted-foreground">{rec.name}</p>
                        </div>
                      </div>
                      <div className="text-center">
                        <p className="text-sm text-muted-foreground">현재 비중</p>
                        <p className="font-semibold">{rec.currentWeight}%</p>
                      </div>
                      <div className="text-center">
                        <p className="text-sm text-muted-foreground">목표 비중</p>
                        <p className="font-semibold">{rec.targetWeight}%</p>
                      </div>
                      <div className="text-center">
                        <div className={`inline-flex px-3 py-1 rounded-full text-sm font-medium ${
                          rec.action === 'BUY'
                            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300'
                            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300'
                        }`}>
                          {rec.action}
                        </div>
                        <p className="text-sm font-semibold mt-1">{rec.amount}</p>
                      </div>
                    </div>
                    <div className="mt-4">
                      <p className="text-sm text-muted-foreground">{rec.reason}</p>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Current Holdings */}
        <motion.section
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.9 }}
        >
          <h3 className="text-2xl font-bold mb-6">현재 보유 종목</h3>
          <Card>
            <CardContent className="p-6">
              <div className="text-center text-muted-foreground">
                <BarChart3 className="h-12 w-12 mx-auto mb-4" />
                <p>보유 종목 데이터를 불러오는 중...</p>
                <p className="text-sm mt-2">실제 포트폴리오 데이터는 API 연동 후 표시됩니다</p>
              </div>
            </CardContent>
          </Card>
        </motion.section>
      </main>
    </div>
  )
}