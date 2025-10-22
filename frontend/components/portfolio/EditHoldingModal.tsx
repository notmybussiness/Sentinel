"use client";

import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal, ModalFooter } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { updateHolding, type UpdateHoldingRequest, type PortfolioHolding } from '@/lib/api/portfolio';

interface EditHoldingModalProps {
  isOpen: boolean;
  onClose: () => void;
  portfolioId: number;
  holding: PortfolioHolding | null;
  onSuccess: () => void;
}

/**
 * EditHoldingModal 컴포넌트
 *
 * Holdings 수정을 위한 Modal
 * - 수량/평균가 수정
 * - API 연동
 */
export function EditHoldingModal({
  isOpen,
  onClose,
  portfolioId,
  holding,
  onSuccess,
}: EditHoldingModalProps) {
  // Form state
  const [quantity, setQuantity] = useState('');
  const [averageCost, setAverageCost] = useState('');

  // Validation errors
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  // Initialize form when holding changes
  useEffect(() => {
    if (holding) {
      setQuantity(holding.quantity.toString());
      setAverageCost(holding.averageCost.toString());
    }
  }, [holding]);

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setQuantity('');
      setAverageCost('');
      setErrors({});
    }
  }, [isOpen]);

  // Update holding mutation
  const { mutate: updateHoldingMutation, isLoading: isSubmitting } = useMutation({
    mutationFn: (data: UpdateHoldingRequest) => {
      if (!holding) throw new Error('No holding selected');
      return updateHolding(portfolioId, holding.id, data);
    },
    onSuccess: () => {
      onSuccess();
      onClose();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      setErrors({
        submit: err.response?.data?.message || '종목 수정에 실패했습니다',
      });
    },
  });

  // Validate form
  const validate = (): boolean => {
    const newErrors: { [key: string]: string } = {};

    const qty = parseFloat(quantity);
    if (!quantity || isNaN(qty) || qty <= 0) {
      newErrors.quantity = '수량은 0보다 큰 숫자여야 합니다';
    }

    const cost = parseFloat(averageCost);
    if (!averageCost || isNaN(cost) || cost <= 0) {
      newErrors.averageCost = '평균 매수가는 0보다 큰 숫자여야 합니다';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle submit
  const handleSubmit = () => {
    if (!validate()) return;

    updateHoldingMutation({
      quantity: parseFloat(quantity),
      averageCost: parseFloat(averageCost),
    });
  };

  if (!holding) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="종목 수정" size="md">
      <div className="space-y-4">
        {/* Symbol Display */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            종목
          </label>
          <div className="p-3 bg-background-secondary rounded-8">
            <div className="font-semibold text-text-primary">
              {holding.symbol}
            </div>
          </div>
        </div>

        {/* Quantity Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            수량
          </label>
          <Input
            type="number"
            placeholder="보유 수량을 입력하세요"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            error={errors.quantity}
            min="0"
            step="0.01"
          />
        </div>

        {/* Average Cost Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            평균 매수가 (KRW)
          </label>
          <Input
            type="number"
            placeholder="평균 매수가를 입력하세요"
            value={averageCost}
            onChange={(e) => setAverageCost(e.target.value)}
            error={errors.averageCost}
            min="0"
            step="100"
          />
        </div>

        {/* Submit Error */}
        {errors.submit && (
          <div className="p-3 bg-accent-red/10 border border-accent-red rounded-8">
            <p className="text-sm text-accent-red">{errors.submit}</p>
          </div>
        )}
      </div>

      {/* Footer */}
      <ModalFooter>
        <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
          취소
        </Button>
        <Button
          variant="primary"
          onClick={handleSubmit}
          disabled={isSubmitting}
        >
          {isSubmitting ? '수정 중...' : '수정 완료'}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
