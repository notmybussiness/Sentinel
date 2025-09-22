'use client'

import React from 'react'
import { motion } from 'framer-motion'
import { TrendingUp, TrendingDown, DollarSign, Activity } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface PortfolioCardProps {
  title: string
  value: string
  change: string
  changePercent: string
  isPositive: boolean
  icon?: React.ReactNode
}

export const PortfolioCard: React.FC<PortfolioCardProps> = ({
  title,
  value,
  change,
  changePercent,
  isPositive,
  icon
}) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      whileHover={{ y: -2 }}
    >
      <Card className="relative overflow-hidden border-l-4 border-l-primary">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            {title}
          </CardTitle>
          <div className="h-4 w-4 text-muted-foreground">
            {icon || <Activity className="h-4 w-4" />}
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{value}</div>
          <div className="flex items-center pt-1">
            <div className={`flex items-center text-xs ${
              isPositive ? 'text-green-600' : 'text-red-600'
            }`}>
              {isPositive ? (
                <TrendingUp className="mr-1 h-3 w-3" />
              ) : (
                <TrendingDown className="mr-1 h-3 w-3" />
              )}
              {changePercent}
            </div>
            <span className="ml-2 text-xs text-muted-foreground">
              {change} from last month
            </span>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  )
}