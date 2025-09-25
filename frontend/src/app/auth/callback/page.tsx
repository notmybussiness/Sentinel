'use client'

import React, { useEffect, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { Activity, CheckCircle, XCircle, Loader2 } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

type AuthStatus = 'loading' | 'success' | 'error'

export default function KakaoCallback() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [message, setMessage] = useState('카카오 로그인 처리 중...')

  useEffect(() => {
    const handleKakaoCallback = async () => {
      try {
        // 토큰이 쿼리 파라미터로 직접 전달되는 경우 (백엔드 리다이렉트)
        const accessToken = searchParams.get('accessToken')
        const refreshToken = searchParams.get('refreshToken')
        const expiresIn = searchParams.get('expiresIn')

        if (accessToken && refreshToken) {
          // 백엔드에서 리다이렉트로 전달된 토큰들을 localStorage에 저장
          localStorage.setItem('accessToken', accessToken)
          localStorage.setItem('refreshToken', refreshToken)

          // 사용자 정보 가져오기
          const userResponse = await fetch('/api/v1/auth/me', {
            headers: {
              'Authorization': `Bearer ${accessToken}`,
            },
          })

          if (userResponse.ok) {
            const userData = await userResponse.json()
            localStorage.setItem('user', JSON.stringify(userData))
            setStatus('success')
            setMessage(`${userData.name}님, 환영합니다!`)
          } else {
            throw new Error('사용자 정보 조회 실패')
          }

          // 2초 후 메인 페이지로 이동
          setTimeout(() => {
            router.push('/')
          }, 2000)
          return
        }

        // 기존 방식: authorization code로 처리
        const code = searchParams.get('code')
        const error = searchParams.get('error')

        if (error) {
          setStatus('error')
          setMessage('카카오 로그인이 취소되었습니다.')
          setTimeout(() => router.push('/'), 3000)
          return
        }

        if (!code) {
          setStatus('error')
          setMessage('인증 코드가 없습니다.')
          setTimeout(() => router.push('/'), 3000)
          return
        }

        // 백엔드로 authorization code 전송
        const response = await fetch('/api/v1/auth/kakao/callback', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ code }),
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()

        // JWT 토큰들을 localStorage에 저장
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        localStorage.setItem('user', JSON.stringify(data.user))

        setStatus('success')
        setMessage(`${data.user.name}님, 환영합니다!`)

        // 2초 후 메인 페이지로 이동
        setTimeout(() => {
          router.push('/')
        }, 2000)

      } catch (error) {
        console.error('카카오 로그인 콜백 처리 실패:', error)
        setStatus('error')
        setMessage('로그인 처리 중 오류가 발생했습니다.')
        setTimeout(() => router.push('/'), 3000)
      }
    }

    handleKakaoCallback()
  }, [searchParams, router])

  const getStatusIcon = () => {
    switch (status) {
      case 'loading':
        return <Loader2 className="h-8 w-8 text-blue-600 animate-spin" />
      case 'success':
        return <CheckCircle className="h-8 w-8 text-green-600" />
      case 'error':
        return <XCircle className="h-8 w-8 text-red-600" />
    }
  }

  const getStatusColor = () => {
    switch (status) {
      case 'loading':
        return 'text-blue-600'
      case 'success':
        return 'text-green-600'
      case 'error':
        return 'text-red-600'
    }
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
                <p className="text-sm text-muted-foreground">카카오 로그인</p>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
              className="flex justify-center"
            >
              {getStatusIcon()}
            </motion.div>

            <motion.p
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className={`text-lg font-medium ${getStatusColor()}`}
            >
              {message}
            </motion.p>

            {status === 'success' && (
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.5 }}
                className="text-sm text-muted-foreground"
              >
                잠시 후 메인 페이지로 이동합니다...
              </motion.p>
            )}

            {status === 'error' && (
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.5 }}
                className="text-sm text-muted-foreground"
              >
                잠시 후 홈 페이지로 이동합니다...
              </motion.p>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </div>
  )
}