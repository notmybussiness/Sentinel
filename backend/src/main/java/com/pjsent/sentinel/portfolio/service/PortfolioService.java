package com.pjsent.sentinel.portfolio.service;

import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.dto.*;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioHoldingRepository;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포트폴리오 서비스
 * 포트폴리오 및 보유 종목 관리를 담당하는 비즈니스 로직 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final MarketDataService marketDataService;

    /**
     * 사용자의 모든 포트폴리오 조회
     */
    public List<PortfolioDto> getPortfoliosByUserId(Long userId) {
        log.info("사용자 포트폴리오 목록 조회. 사용자 ID: {}", userId);
        
        List<Portfolio> portfolios = portfolioRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return portfolios.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 포트폴리오 조회
     */
    public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
        log.info("포트폴리오 조회. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        return convertToDto(portfolio);
    }

    /**
     * 포트폴리오 생성
     */
    @Transactional
    public PortfolioDto createPortfolio(Long userId, CreatePortfolioRequest request) {
        log.info("포트폴리오 생성. 사용자 ID: {}, 이름: {}", userId, request.getName());
        
        // 이름 중복 체크
        if (portfolioRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 포트폴리오 이름입니다: " + request.getName());
        }
        
        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("포트폴리오 생성 완료. ID: {}, 이름: {}", savedPortfolio.getId(), savedPortfolio.getName());
        
        return convertToDto(savedPortfolio);
    }

    /**
     * 포트폴리오 수정
     */
    @Transactional
    public PortfolioDto updatePortfolio(Long portfolioId, Long userId, UpdatePortfolioRequest request) {
        log.info("포트폴리오 수정. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        // 이름 중복 체크 (자기 자신 제외)
        if (!portfolio.getName().equals(request.getName()) && 
            portfolioRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 포트폴리오 이름입니다: " + request.getName());
        }
        
        portfolio.updatePortfolio(request.getName(), request.getDescription());
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        
        log.info("포트폴리오 수정 완료. ID: {}, 이름: {}", savedPortfolio.getId(), savedPortfolio.getName());
        return convertToDto(savedPortfolio);
    }

    /**
     * 포트폴리오 삭제
     */
    @Transactional
    public void deletePortfolio(Long portfolioId, Long userId) {
        log.info("포트폴리오 삭제. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        portfolioRepository.delete(portfolio);
        log.info("포트폴리오 삭제 완료. ID: {}", portfolioId);
    }

    /**
     * 보유 종목 추가
     */
    @Transactional
    public PortfolioHoldingDto addHolding(Long portfolioId, Long userId, AddHoldingRequest request) {
        log.info("보유 종목 추가. 포트폴리오 ID: {}, 심볼: {}, 수량: {}", 
                portfolioId, request.getSymbol(), request.getQuantity());
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        // 이미 존재하는 종목인지 확인
        if (holdingRepository.existsByPortfolioIdAndSymbol(portfolioId, request.getSymbol())) {
            throw new IllegalArgumentException("이미 존재하는 보유 종목입니다: " + request.getSymbol());
        }
        
        PortfolioHolding holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol(request.getSymbol())
                .quantity(request.getQuantity())
                .averageCost(request.getAverageCost())
                .build();
        
        // 현재 가격 조회 및 설정
        try {
            var stockPrice = marketDataService.getStockPrice(request.getSymbol());
            holding.updateCurrentPrice(BigDecimal.valueOf(stockPrice.getPrice()));
        } catch (Exception e) {
            log.warn("현재 가격 조회 실패. 심볼: {}, 오류: {}", request.getSymbol(), e.getMessage());
            // 현재 가격 조회 실패해도 보유 종목은 생성
        }
        
        PortfolioHolding savedHolding = holdingRepository.save(holding);
        portfolio.addHolding(savedHolding);
        portfolio.recalculate();
        portfolioRepository.save(portfolio);
        
        log.info("보유 종목 추가 완료. ID: {}, 심볼: {}", savedHolding.getId(), savedHolding.getSymbol());
        return convertToHoldingDto(savedHolding);
    }

    /**
     * 보유 종목 수정
     */
    @Transactional
    public PortfolioHoldingDto updateHolding(Long portfolioId, Long holdingId, Long userId, UpdateHoldingRequest request) {
        log.info("보유 종목 수정. 포트폴리오 ID: {}, 보유 종목 ID: {}, 수량: {}", 
                portfolioId, holdingId, request.getQuantity());
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("보유 종목을 찾을 수 없습니다. ID: " + holdingId));
        
        if (!holding.getPortfolio().getId().equals(portfolioId)) {
            throw new IllegalArgumentException("해당 포트폴리오의 보유 종목이 아닙니다.");
        }
        
        holding.updateHolding(request.getQuantity(), request.getAverageCost());
        PortfolioHolding savedHolding = holdingRepository.save(holding);
        
        portfolio.recalculate();
        portfolioRepository.save(portfolio);
        
        log.info("보유 종목 수정 완료. ID: {}, 심볼: {}", savedHolding.getId(), savedHolding.getSymbol());
        return convertToHoldingDto(savedHolding);
    }

    /**
     * 보유 종목 삭제
     */
    @Transactional
    public void deleteHolding(Long portfolioId, Long holdingId, Long userId) {
        log.info("보유 종목 삭제. 포트폴리오 ID: {}, 보유 종목 ID: {}", portfolioId, holdingId);
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("보유 종목을 찾을 수 없습니다. ID: " + holdingId));
        
        if (!holding.getPortfolio().getId().equals(portfolioId)) {
            throw new IllegalArgumentException("해당 포트폴리오의 보유 종목이 아닙니다.");
        }
        
        portfolio.removeHolding(holding);
        holdingRepository.delete(holding);
        portfolio.recalculate();
        portfolioRepository.save(portfolio);
        
        log.info("보유 종목 삭제 완료. ID: {}, 심볼: {}", holdingId, holding.getSymbol());
    }

    /**
     * 포트폴리오 재계산 (현재 가격 반영)
     */
    @Transactional
    public PortfolioDto recalculatePortfolio(Long portfolioId, Long userId) {
        log.info("포트폴리오 재계산. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
        
        // 모든 보유 종목의 현재 가격 업데이트
        for (PortfolioHolding holding : portfolio.getHoldings()) {
            try {
                var stockPrice = marketDataService.getStockPrice(holding.getSymbol());
                holding.updateCurrentPrice(BigDecimal.valueOf(stockPrice.getPrice()));
            } catch (Exception e) {
                log.warn("현재 가격 조회 실패. 심볼: {}, 오류: {}", holding.getSymbol(), e.getMessage());
            }
        }
        
        portfolio.recalculate();
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        
        log.info("포트폴리오 재계산 완료. ID: {}, 총 가치: {}", 
                savedPortfolio.getId(), savedPortfolio.getTotalValue());
        
        return convertToDto(savedPortfolio);
    }

    /**
     * Portfolio 엔티티를 DTO로 변환
     */
    private PortfolioDto convertToDto(Portfolio portfolio) {
        List<PortfolioHoldingDto> holdingDtos = portfolio.getHoldings().stream()
                .map(this::convertToHoldingDto)
                .collect(Collectors.toList());
        
        return PortfolioDto.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUserId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .totalValue(portfolio.getTotalValue())
                .totalCost(portfolio.getTotalCost())
                .totalGainLoss(portfolio.getTotalGainLoss())
                .totalGainLossPercent(portfolio.getTotalGainLossPercent())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .holdings(holdingDtos)
                .build();
    }

    /**
     * PortfolioHolding 엔티티를 DTO로 변환
     */
    private PortfolioHoldingDto convertToHoldingDto(PortfolioHolding holding) {
        return PortfolioHoldingDto.builder()
                .id(holding.getId())
                .portfolioId(holding.getPortfolio().getId())
                .symbol(holding.getSymbol())
                .quantity(holding.getQuantity())
                .averageCost(holding.getAverageCost())
                .currentPrice(holding.getCurrentPrice())
                .marketValue(holding.getMarketValue())
                .totalCost(holding.getTotalCost())
                .gainLoss(holding.getGainLoss())
                .gainLossPercent(holding.getGainLossPercent())
                .createdAt(holding.getCreatedAt())
                .updatedAt(holding.getUpdatedAt())
                .build();
    }
}
