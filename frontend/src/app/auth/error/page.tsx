'use client'

import React, { useEffect, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { Activity, XCircle, Home, RotateCcw } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

export default function AuthError() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const [message, setMessage] = useState('로그인 중 오류가 발생했습니다.')

  useEffect(() => {
    const errorMessage = searchParams.get('message')
    if (errorMessage) {
      setMessage(decodeURIComponent(errorMessage))
    }
  }, [searchParams])

  const handleGoHome = () => {
    router.push('/')
  }

  const handleRetry = () => {
    router.push('/') // 홈으로 가서 다시 로그인 시도
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.3 }}
        className="w-full max-w-md"
      >
        <Card className="text-center">
          <CardHeader>
            <div className="flex items-center justify-center space-x-3 mb-4">
              <div className="h-10 w-10 bg-gradient-to-r from-blue-600 to-purple-600 rounded-lg flex items-center justify-center">
                <Activity className="h-6 w-6 text-white" />
              </div>
              <div>
                <CardTitle className="text-xl">Project Sentinel</CardTitle>
                <p className="text-sm text-muted-foreground">로그인 오류</p>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
              className="flex justify-center"
            >
              <XCircle className="h-12 w-12 text-red-600" />
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="space-y-2"
            >
              <h3 className="text-lg font-semibold text-red-600">
                로그인 실패
              </h3>
              <p className="text-muted-foreground text-sm">
                {message}
              </p>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
              className="flex flex-col space-y-3"
            >
              <Button
                onClick={handleRetry}
                className="w-full"
                variant="default"
              >
                <RotateCcw className="h-4 w-4 mr-2" />
                다시 시도
              </Button>

              <Button
                onClick={handleGoHome}
                className="w-full"
                variant="outline"
              >
                <Home className="h-4 w-4 mr-2" />
                홈으로 이동
              </Button>
            </motion.div>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.7 }}
              className="text-xs text-muted-foreground"
            >
              문제가 지속되면 관리자에게 문의하세요.
            </motion.p>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  )
}