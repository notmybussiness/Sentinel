package com.pjsent.sentinel.portfolio.service;

import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.dto.*;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import com.pjsent.sentinel.portfolio.mapper.PortfolioMapper;
import com.pjsent.sentinel.portfolio.repository.PortfolioHoldingRepository;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오 서비스
 * 포트폴리오 및 보유 종목 관리를 담당하는 비즈니스 로직 서비스
 * 
 * 리팩토링 (TDD Phase 3):
 * - PortfolioMapper 컴포넌트로 매핑 로직 분리
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final MarketDataService marketDataService;
    private final CryptoDataService cryptoDataService;
    private final PortfolioMapper portfolioMapper;

    /**
     * 사용자의 모든 포트폴리오 조회
     */
    public List<PortfolioDto> getPortfoliosByUserId(Long userId) {
        log.info("사용자 포트폴리오 목록 조회. 사용자 ID: {}", userId);

        List<Portfolio> portfolios = portfolioRepository.findByUserIdOrderByCreatedAtDescWithHoldings(userId);

        return portfolios.stream()
                .map(portfolioMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 포트폴리오 조회
     *
     * Phase 4a: Read/Write 분리
     * - 조회 시 외부 API 호출 제거 (updatePortfolioPrices 제거)
     * - DB 조회만 수행 → 응답 속도 수십 ms로 개선
     * - 가격 업데이트는 PortfolioPriceScheduler가 백그라운드에서 처리 (5분마다)
     *
     * Phase 7: Redis 캐시 최적화
     * - @Transactional 제거: 단일 조회 쿼리만 실행, 트랜잭션 불필요
     * - @EntityGraph 사용으로 Lazy Loading 없음 (한 번에 JOIN FETCH)
     * - 캐시 히트 시 DB 커넥션 획득 불필요 → HikariCP 커넥션 풀 효율 향상
     */
    @Cacheable(value = "portfolios", key = "#portfolioId")
    public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
        log.info("포트폴리오 조회. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);

        Portfolio portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));

        // ✅ 순수 DB 조회만 수행 (외부 API 호출 없음)
        return portfolioMapper.toDto(portfolio);
    }

    /**
     * 포트폴리오 가격 업데이트 (내부용)
     *
     * @deprecated Phase 4a에서 PortfolioPriceScheduler로 이동
     *             조회 시 실시간 업데이트 대신 스케줄러가 백그라운드에서 주기적으로 업데이트
     *             수동 재계산이 필요한 경우 recalculatePortfolio() 사용
     */
    @Deprecated
    private void updatePortfolioPrices(Portfolio portfolio) {
        for (PortfolioHolding holding : portfolio.getHoldings()) {
            try {
                if (holding.getAssetType() == AssetType.CRYPTO) {
                    CryptoPriceDto cryptoPrice = cryptoDataService.getCryptoPrice(
                            holding.getSymbol(),
                            holding.getBaseCurrency());
                    holding.updateCurrentPrice(BigDecimal.valueOf(cryptoPrice.getPrice()));
                } else {
                    var stockPrice = marketDataService.getStockPrice(holding.getSymbol());
                    holding.updateCurrentPrice(BigDecimal.valueOf(stockPrice.getPrice()));
                }
            } catch (Exception e) {
                log.warn("가격 업데이트 실패. 심볼: {}, 타입: {}, 오류: {}",
                        holding.getSymbol(), holding.getAssetType(), e.getMessage());
                // 가격 조회 실패해도 기존 가격 유지
            }
        }

        portfolio.recalculate();
        portfolioRepository.save(portfolio);
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

        return portfolioMapper.toDto(savedPortfolio);
    }

    /**
     * 포트폴리오 수정
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
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
        return portfolioMapper.toDto(savedPortfolio);
    }

    /**
     * 포트폴리오 삭제
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
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
    @CacheEvict(value = "portfolios", key = "#portfolioId")
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
                .assetType(AssetType.valueOf(request.getAssetType()))
                .baseCurrency(request.getBaseCurrency())
                .build();

        // 현재 가격 조회 및 설정 (자산 타입에 따라 다른 API 사용)
        try {
            if (holding.getAssetType() == AssetType.CRYPTO) {
                CryptoPriceDto cryptoPrice = cryptoDataService.getCryptoPrice(
                        request.getSymbol(),
                        request.getBaseCurrency());
                holding.updateCurrentPrice(BigDecimal.valueOf(cryptoPrice.getPrice()));
            } else {
                var stockPrice = marketDataService.getStockPrice(request.getSymbol());
                holding.updateCurrentPrice(BigDecimal.valueOf(stockPrice.getPrice()));
            }
        } catch (Exception e) {
            log.warn("현재 가격 조회 실패. 심볼: {}, 타입: {}, 오류: {}",
                    request.getSymbol(), request.getAssetType(), e.getMessage());
            // 현재 가격 조회 실패해도 보유 종목은 생성
        }

        PortfolioHolding savedHolding = holdingRepository.save(holding);
        portfolio.addHolding(savedHolding);
        portfolio.recalculate();
        portfolioRepository.save(portfolio);

        log.info("보유 종목 추가 완료. ID: {}, 심볼: {}", savedHolding.getId(), savedHolding.getSymbol());
        return portfolioMapper.toHoldingDto(savedHolding);
    }

    /**
     * 보유 종목 수정
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public PortfolioHoldingDto updateHolding(Long portfolioId, Long holdingId, Long userId,
            UpdateHoldingRequest request) {
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

        // 암호화폐인 경우 baseCurrency 업데이트
        if (request.getBaseCurrency() != null) {
            holding.updateBaseCurrency(request.getBaseCurrency());
        }

        PortfolioHolding savedHolding = holdingRepository.save(holding);

        portfolio.recalculate();
        portfolioRepository.save(portfolio);

        log.info("보유 종목 수정 완료. ID: {}, 심볼: {}", savedHolding.getId(), savedHolding.getSymbol());
        return portfolioMapper.toHoldingDto(savedHolding);
    }

    /**
     * 보유 종목 삭제
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
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
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public PortfolioDto recalculatePortfolio(Long portfolioId, Long userId) {
        log.info("포트폴리오 재계산. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));

        // 모든 보유 종목의 현재 가격 업데이트 (자산 타입에 따라 다른 API 사용)
        for (PortfolioHolding holding : portfolio.getHoldings()) {
            try {
                if (holding.getAssetType() == AssetType.CRYPTO) {
                    CryptoPriceDto cryptoPrice = cryptoDataService.getCryptoPrice(
                            holding.getSymbol(),
                            holding.getBaseCurrency());
                    holding.updateCurrentPrice(BigDecimal.valueOf(cryptoPrice.getPrice()));
                } else {
                    var stockPrice = marketDataService.getStockPrice(holding.getSymbol());
                    holding.updateCurrentPrice(BigDecimal.valueOf(stockPrice.getPrice()));
                }
            } catch (Exception e) {
                log.warn("현재 가격 조회 실패. 심볼: {}, 타입: {}, 오류: {}",
                        holding.getSymbol(), holding.getAssetType(), e.getMessage());
            }
        }

        portfolio.recalculate();
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        log.info("포트폴리오 재계산 완료. ID: {}, 총 가치: {}",
                savedPortfolio.getId(), savedPortfolio.getTotalValue());

        return portfolioMapper.toDto(savedPortfolio);
    }
}
