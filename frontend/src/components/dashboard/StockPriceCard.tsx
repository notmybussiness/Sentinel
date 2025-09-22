'use client'

import React from 'react'
import { motion } from 'framer-motion'
import { TrendingUp, TrendingDown } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface StockData {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  marketCap?: string
  volume?: string
}

interface StockPriceCardProps {
  stock: StockData
}

export const StockPriceCard: React.FC<StockPriceCardProps> = ({ stock }) => {
  const isPositive = stock.change >= 0

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      whileHover={{ scale: 1.02 }}
    >
      <Card className="cursor-pointer hover:shadow-md transition-shadow">
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg font-bold">{stock.symbol}</CardTitle>
              <p className="text-sm text-muted-foreground truncate">
                {stock.name}
              </p>
            </div>
            <div className={`p-2 rounded-full ${
              isPositive ? 'bg-green-100' : 'bg-red-100'
            }`}>
              {isPositive ? (
                <TrendingUp className={`h-4 w-4 ${
                  isPositive ? 'text-green-600' : 'text-red-600'
                }`} />
              ) : (
                <TrendingDown className={`h-4 w-4 ${
                  isPositive ? 'text-green-600' : 'text-red-600'
                }`} />
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <div className="flex items-baseline justify-between">
              <span className="text-2xl font-bold">
                ${stock.price.toFixed(2)}
              </span>
              <div className={`text-sm font-medium ${
                isPositive ? 'text-green-600' : 'text-red-600'
              }`}>
                {isPositive ? '+' : ''}{stock.change.toFixed(2)}
                <span className="ml-1">
                  ({isPositive ? '+' : ''}{stock.changePercent.toFixed(2)}%)
                </span>
              </div>
            </div>
            
            {(stock.marketCap || stock.volume) && (
              <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground pt-2 border-t">
                {stock.marketCap && (
                  <div>
                    <span className="font-medium">Market Cap</span>
                    <div>{stock.marketCap}</div>
                  </div>
                )}
                {stock.volume && (
                  <div>
                    <span className="font-medium">Volume</span>
                    <div>{stock.volume}</div>
                  </div>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </motion.div>
  )
}