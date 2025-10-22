"use client";

import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import {
  analyzePortfolio,
  analysisTypeLabels,
  riskLevelColors,
  riskLevelLabels,
} from '@/lib/api/ai';
import type { AnalysisRequest, AnalysisResponse } from '@/types';

interface PortfolioAnalysisModalProps {
  isOpen: boolean;
  onClose: () => void;
  portfolioId: number;
}

/**
 * 포트폴리오 AI 분석 Modal
 * Gemini AI를 활용한 포트폴리오 분석 및 투자 제안
 */
export function PortfolioAnalysisModal({
  isOpen,
  onClose,
  portfolioId,
}: PortfolioAnalysisModalProps) {
  const [selectedType, setSelectedType] = useState<AnalysisRequest['analysisType']>('OVERVIEW');
  const [analysisResult, setAnalysisResult] = useState<AnalysisResponse | null>(null);

  // AI 분석 Mutation
  const analysisMutation = useMutation({
    mutationFn: (request: AnalysisRequest) => analyzePortfolio(request),
    onSuccess: (data) => {
      setAnalysisResult(data);
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'AI 분석에 실패했습니다');
    },
  });

  // 분석 실행
  const handleAnalyze = () => {
    setAnalysisResult(null);
    analysisMutation.mutate({
      portfolioId,
      analysisType: selectedType,
      includeMarketContext: true,
    });
  };

  // Modal 닫기
  const handleClose = () => {
    setAnalysisResult(null);
    setSelectedType('OVERVIEW');
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="AI 포트폴리오 분석">
      <div className="space-y-4">
        {/* 분석 타입 선택 */}
        {!analysisResult && (
          <div>
            <label className="block text-text-primary text-sm font-medium mb-2">
              분석 유형 선택
            </label>
            <div className="grid grid-cols-1 gap-2">
              {(Object.keys(analysisTypeLabels) as Array<AnalysisRequest['analysisType']>).map(
                (type) => (
                  <button
                    key={type}
                    onClick={() => setSelectedType(type)}
                    className={`p-3 rounded-8 text-left transition-colors ${
                      selectedType === type
                        ? 'bg-brand/20 border border-brand text-brand'
                        : 'bg-background-secondary border border-transparent text-text-primary hover:bg-background-tertiary'
                    }`}
                  >
                    <div className="font-medium">{analysisTypeLabels[type]}</div>
                    <div className="text-sm text-text-tertiary mt-0.5">
                      {type === 'OVERVIEW' && 'AI가 포트폴리오 전반을 분석합니다'}
                      {type === 'DIVERSIFICATION' && '자산 분산 상태를 평가합니다'}
                      {type === 'RISK' && '포트폴리오의 리스크를 측정합니다'}
                      {type === 'PERFORMANCE' && '수익률과 성과를 분석합니다'}
                      {type === 'RECOMMENDATION' && '구체적인 투자 제안을 제공합니다'}
                    </div>
                  </button>
                )
              )}
            </div>
          </div>
        )}

        {/* 분석 중 상태 */}
        {analysisMutation.isPending && (
          <div className="text-center py-8">
            <div className="text-6xl mb-4 animate-pulse">🤖</div>
            <h3 className="text-text-primary text-xl font-semibold mb-2">AI 분석 중...</h3>
            <p className="text-text-tertiary">
              Gemini AI가 포트폴리오를 분석하고 있습니다
            </p>
          </div>
        )}

        {/* 분석 결과 */}
        {analysisResult && (
          <div className="space-y-4">
            {/* 요약 */}
            <Card padding="sm">
              <h3 className="text-text-primary font-semibold mb-2">📝 분석 요약</h3>
              <p className="text-text-secondary text-sm leading-relaxed">
                {analysisResult.summary}
              </p>
            </Card>

            {/* 리스크 & 다각화 점수 */}
            <div className="grid grid-cols-2 gap-3">
              <Card padding="sm">
                <div className="text-text-tertiary text-sm mb-1">리스크 수준</div>
                <div className={`text-2xl font-bold ${riskLevelColors[analysisResult.riskLevel]}`}>
                  {riskLevelLabels[analysisResult.riskLevel]}
                </div>
              </Card>
              <Card padding="sm">
                <div className="text-text-tertiary text-sm mb-1">다각화 점수</div>
                <div className="text-2xl font-bold text-brand">
                  {analysisResult.diversificationScore.toFixed(0)}점
                </div>
              </Card>
            </div>

            {/* 주요 인사이트 */}
            <Card padding="sm">
              <h3 className="text-text-primary font-semibold mb-2">💡 주요 인사이트</h3>
              <ul className="space-y-2">
                {analysisResult.insights.map((insight, index) => (
                  <li key={index} className="flex gap-2">
                    <span className="text-brand flex-shrink-0">•</span>
                    <span className="text-text-secondary text-sm">{insight}</span>
                  </li>
                ))}
              </ul>
            </Card>

            {/* 투자 제안 */}
            <Card padding="sm">
              <h3 className="text-text-primary font-semibold mb-2">🎯 투자 제안</h3>
              <ul className="space-y-2">
                {analysisResult.recommendations.map((recommendation, index) => (
                  <li key={index} className="flex gap-2">
                    <span className="text-success flex-shrink-0">✓</span>
                    <span className="text-text-secondary text-sm">{recommendation}</span>
                  </li>
                ))}
              </ul>
            </Card>

            {/* 분석 정보 */}
            <div className="text-center text-text-tertiary text-mini">
              분석 시각: {new Date(analysisResult.analyzedAt).toLocaleString('ko-KR')}
              <br />
              Powered by Gemini AI
            </div>
          </div>
        )}

        {/* 액션 버튼 */}
        <div className="flex gap-2 justify-end">
          {!analysisResult && (
            <>
              <Button variant="secondary" size="md" onClick={handleClose}>
                취소
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={handleAnalyze}
                disabled={analysisMutation.isPending}
              >
                {analysisMutation.isPending ? '분석 중...' : 'AI 분석 시작'}
              </Button>
            </>
          )}
          {analysisResult && (
            <>
              <Button
                variant="secondary"
                size="md"
                onClick={() => {
                  setAnalysisResult(null);
                }}
              >
                다시 분석
              </Button>
              <Button variant="primary" size="md" onClick={handleClose}>
                닫기
              </Button>
            </>
          )}
        </div>
      </div>
    </Modal>
  );
}
