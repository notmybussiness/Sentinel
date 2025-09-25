'use client'

import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { DollarSign, TrendingUp, BarChart3, Shield, User, LogOut } from 'lucide-react'
import { PortfolioCard } from '@/components/dashboard/PortfolioCard'
import { StockPriceCard } from '@/components/dashboard/StockPriceCard'
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

const mockStockData = [
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

export default function Dashboard() {
  const router = useRouter()
  const [user, setUser] = useState<any>(null)
  const [stockData, setStockData] = useState(mockStockData)

  useEffect(() => {
    // 로그인 확인
    const accessToken = localStorage.getItem('accessToken')
    const userData = localStorage.getItem('user')

    if (!accessToken || !userData) {
      router.push('/')
      return
    }

    setUser(JSON.parse(userData))

    // AAPL 실제 데이터 가져오기
    const fetchAaplData = async () => {
      try {
        const response = await fetch('/api/v1/market/price/AAPL', {
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        })
        if (response.ok) {
          const data = await response.json()
          const aaplStock = {
            symbol: "AAPL",
            name: "Apple Inc.",
            price: data.price,
            change: data.change,
            changePercent: data.changePercent,
            marketCap: "$2.7T",
            volume: "45.2M"
          }
          setStockData([aaplStock, ...mockStockData])
        }
      } catch (error) {
        console.error('AAPL 데이터 로딩 실패:', error)
        const aaplStock = {
          symbol: "AAPL",
          name: "Apple Inc.",
          price: 175.43,
          change: 2.34,
          changePercent: 1.35,
          marketCap: "$2.7T",
          volume: "45.2M"
        }
        setStockData([aaplStock, ...mockStockData])
      }
    }

    fetchAaplData()
  }, [router])

  const handleLogout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    router.push('/')
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin h-8 w-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-4"></div>
          <p>로딩 중...</p>
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
                <BarChart3 className="h-5 w-5 text-white" />
              </div>
              <div>
                <h1 className="text-xl font-bold">Project Sentinel</h1>
                <p className="text-sm text-muted-foreground">투자 대시보드</p>
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
              <Button variant="outline" size="sm" onClick={handleLogout}>
                <LogOut className="h-4 w-4 mr-2" />
                로그아웃
              </Button>
            </div>
          </div>
        </div>
      </motion.header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="mb-8"
        >
          <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
            환영합니다, {user.name}님! 👋
          </h2>
          <p className="text-lg text-muted-foreground">
            오늘도 데이터 기반의 현명한 투자를 시작해보세요.
          </p>
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
      </main>
    </div>
  )
}