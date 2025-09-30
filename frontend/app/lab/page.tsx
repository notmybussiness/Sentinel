"use client";

import React, { useState } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Select, SelectOption } from '@/components/ui/Select';
import { Input } from '@/components/ui/Input';
import { MockDataBadge } from '@/components/common/MockDataBadge';
import { useAuth } from '@/contexts/AuthContext';
import { mockUserPortfolios } from '@/lib/mockData';
import { useRouter } from 'next/navigation';

export default function LabPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const [selectedPortfolio, setSelectedPortfolio] = useState('');
  const [startDate, setStartDate] = useState('2020-01-01');
  const [endDate, setEndDate] = useState('2024-12-31');
  const [initialInvestment, setInitialInvestment] = useState('10000000');

  if (!isAuthenticated) {
    router.push('/login');
    return null;
  }

  const portfolioOptions: SelectOption[] = mockUserPortfolios.map((p) => ({
    value: p.id.toString(),
    label: p.name,
  }));

  const handleRunBacktest = () => {
    if (!selectedPortfolio) {
      alert('포트폴리오를 선택해주세요');
      return;
    }
    alert('백테스팅 실행 (준비 중)\n\n선택된 포트폴리오: ' + selectedPortfolio);
  };

  return (
    <div className="min-h-screen bg-background-primary">
      <PageHeader
        title="실험실"
        subtitle="포트폴리오 백테스팅 및 시뮬레이션"
      />

      <div className="max-w-6xl mx-auto px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* 설정 패널 */}
          <div className="lg:col-span-1">
            <Card padding="sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-text-primary font-semibold">
                  백테스팅 설정
                </h3>
                <MockDataBadge show={true} size="sm" />
              </div>

              <div className="space-y-3">
                {/* 포트폴리오 선택 */}
                <div>
                  <label className="block text-text-secondary text-small mb-2">
                    포트폴리오 선택
                  </label>
                  <Select
                    options={portfolioOptions}
                    value={selectedPortfolio}
                    onChange={setSelectedPortfolio}
                    placeholder="포트폴리오를 선택하세요"
                  />
                </div>

                {/* 기간 설정 */}
                <div>
                  <label className="block text-text-secondary text-small mb-2">
                    시작 날짜
                  </label>
                  <Input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                  />
                </div>

                <div>
                  <label className="block text-text-secondary text-small mb-2">
                    종료 날짜
                  </label>
                  <Input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                  />
                </div>

                {/* 초기 투자금 */}
                <div>
                  <label className="block text-text-secondary text-small mb-2">
                    초기 투자금 (원)
                  </label>
                  <Input
                    type="number"
                    value={initialInvestment}
                    onChange={(e) => setInitialInvestment(e.target.value)}
                    placeholder="10000000"
                  />
                </div>

                {/* 실행 버튼 */}
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full"
                  onClick={handleRunBacktest}
                >
                  백테스팅 실행
                </Button>
              </div>
            </Card>

            {/* 안내 */}
            <Card padding="sm" className="mt-4">
              <h4 className="text-text-primary font-semibold mb-2">
                💡 실험실이란?
              </h4>
              <div className="text-text-tertiary text-mini space-y-1.5">
                <p>
                  과거 데이터를 기반으로 포트폴리오의 성과를 시뮬레이션합니다.
                </p>
                <p>
                  다양한 전략을 테스트하고 최적의 투자 방법을 찾아보세요.
                </p>
              </div>
            </Card>
          </div>

          {/* 결과 패널 */}
          <div className="lg:col-span-2">
            <Card padding="sm">
              <h3 className="text-text-primary font-semibold mb-4">
                백테스팅 결과
              </h3>

              <div className="text-center py-16">
                <div className="text-6xl mb-4">🧪</div>
                <h4 className="text-text-primary text-xl font-semibold mb-2">
                  백테스팅을 실행해주세요
                </h4>
                <p className="text-text-tertiary">
                  왼쪽 패널에서 포트폴리오와 기간을 설정하고
                  <br />
                  백테스팅을 실행하면 결과가 여기에 표시됩니다
                </p>
              </div>
            </Card>

            {/* 추가 정보 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-4">
              <Card padding="sm">
                <div className="text-center">
                  <div className="text-2xl mb-1.5">📊</div>
                  <h5 className="text-text-primary font-semibold mb-0.5 text-sm">
                    수익률 분석
                  </h5>
                  <p className="text-text-tertiary text-mini">
                    연간/누적 수익률
                  </p>
                </div>
              </Card>
              <Card padding="sm">
                <div className="text-center">
                  <div className="text-2xl mb-1.5">📉</div>
                  <h5 className="text-text-primary font-semibold mb-0.5 text-sm">
                    리스크 지표
                  </h5>
                  <p className="text-text-tertiary text-mini">
                    변동성, MDD
                  </p>
                </div>
              </Card>
              <Card padding="sm">
                <div className="text-center">
                  <div className="text-2xl mb-1.5">⚖️</div>
                  <h5 className="text-text-primary font-semibold mb-0.5 text-sm">
                    샤프 비율
                  </h5>
                  <p className="text-text-tertiary text-mini">
                    위험 대비 수익
                  </p>
                </div>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}