"use client";

import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal, ModalFooter } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { createPortfolio, type CreatePortfolioRequest } from '@/lib/api/portfolio';

interface CreatePortfolioModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * CreatePortfolioModal 컴포넌트
 *
 * 새 포트폴리오 생성을 위한 Modal
 * - 이름/설명 입력
 * - API 연동
 */
export function CreatePortfolioModal({
  isOpen,
  onClose,
  onSuccess,
}: CreatePortfolioModalProps) {
  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  // Validation errors
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setName('');
      setDescription('');
      setErrors({});
    }
  }, [isOpen]);

  // Create portfolio mutation
  const { mutate: createPortfolioMutation, isLoading: isSubmitting } = useMutation({
    mutationFn: (data: CreatePortfolioRequest) => createPortfolio(data),
    onSuccess: () => {
      onSuccess();
      onClose();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      setErrors({
        submit: err.response?.data?.message || '포트폴리오 생성에 실패했습니다',
      });
    },
  });

  // Validate form
  const validate = (): boolean => {
    const newErrors: { [key: string]: string } = {};

    if (!name || name.trim().length === 0) {
      newErrors.name = '포트폴리오 이름을 입력해주세요';
    } else if (name.length > 50) {
      newErrors.name = '포트폴리오 이름은 50자 이하여야 합니다';
    }

    if (description && description.length > 200) {
      newErrors.description = '설명은 200자 이하여야 합니다';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle submit
  const handleSubmit = () => {
    if (!validate()) return;

    createPortfolioMutation({
      name: name.trim(),
      description: description.trim() || undefined,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="새 포트폴리오 생성" size="md">
      <div className="space-y-4">
        {/* Name Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            포트폴리오 이름 *
          </label>
          <Input
            type="text"
            placeholder="예: 미국 주식 포트폴리오"
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={errors.name}
            maxLength={50}
          />
          <p className="text-text-quaternary text-micro mt-1">
            {name.length}/50자
          </p>
        </div>

        {/* Description Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            설명 (선택)
          </label>
          <textarea
            className="w-full px-3 py-2 bg-background-secondary border border-border-primary rounded-8 text-text-primary placeholder-text-tertiary focus:outline-none focus:border-brand transition-colors resize-none"
            placeholder="예: 미국 대형주 중심의 장기 투자 포트폴리오"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            maxLength={200}
          />
          {errors.description && (
            <p className="text-accent-red text-mini mt-1">{errors.description}</p>
          )}
          <p className="text-text-quaternary text-micro mt-1">
            {description.length}/200자
          </p>
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
          disabled={isSubmitting || !name.trim()}
        >
          {isSubmitting ? '생성 중...' : '포트폴리오 생성'}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
