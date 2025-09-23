package com.pjsent.sentinel.portfolio.controller;

import com.pjsent.sentinel.portfolio.dto.*;
import com.pjsent.sentinel.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 포트폴리오 컨트롤러
 * 포트폴리오 관련 REST API 엔드포인트를 제공
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * 사용자의 모든 포트폴리오 조회
     */
    @GetMapping
    public ResponseEntity<List<PortfolioDto>> getPortfolios(@AuthenticationPrincipal Long userId) {
        log.info("포트폴리오 목록 조회 요청");
        
        // JWT 사용자 ID 추출 (향후 구현)
        // Long userId = jwtService.getUserIdFromToken(authorization.replace("Bearer ", ""));
        // 임시로 하드코딩 (실제 구현 시 JWT 추출)
        // Long userId = 1L;
        
        List<PortfolioDto> portfolios = portfolioService.getPortfoliosByUserId(userId);
        
        return ResponseEntity.ok(portfolios);
    }

    /**
     * 특정 포트폴리오 조회
     */
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioDto> getPortfolio(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal Long userId) {
        log.info("포트폴리오 조회 요청. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        PortfolioDto portfolio = portfolioService.getPortfolioById(portfolioId, userId);
        
        return ResponseEntity.ok(portfolio);
    }

    /**
     * 포트폴리오 생성
     */
    @PostMapping
    public ResponseEntity<PortfolioDto> createPortfolio(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePortfolioRequest request) {
        log.info("포트폴리오 생성 요청. 사용자 ID: {}, 이름: {}", userId, request.getName());
        
        PortfolioDto portfolio = portfolioService.createPortfolio(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
    }

    /**
     * 포트폴리오 수정
     */
    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioDto> updatePortfolio(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        log.info("포트폴리오 수정 요청. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        PortfolioDto portfolio = portfolioService.updatePortfolio(portfolioId, userId, request);
        
        return ResponseEntity.ok(portfolio);
    }

    /**
     * 포트폴리오 삭제
     */
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal Long userId) {
        log.info("포트폴리오 삭제 요청. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        portfolioService.deletePortfolio(portfolioId, userId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 보유 종목 추가
     */
    @PostMapping("/{portfolioId}/holdings")
    public ResponseEntity<PortfolioHoldingDto> addHolding(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AddHoldingRequest request) {
        log.info("보유 종목 추가 요청. 포트폴리오 ID: {}, 심볼: {}", portfolioId, request.getSymbol());
        
        PortfolioHoldingDto holding = portfolioService.addHolding(portfolioId, userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(holding);
    }

    /**
     * 보유 종목 수정
     */
    @PutMapping("/{portfolioId}/holdings/{holdingId}")
    public ResponseEntity<PortfolioHoldingDto> updateHolding(
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateHoldingRequest request) {
        log.info("보유 종목 수정 요청. 포트폴리오 ID: {}, 보유 종목 ID: {}", portfolioId, holdingId);
        
        PortfolioHoldingDto holding = portfolioService.updateHolding(portfolioId, holdingId, userId, request);
        
        return ResponseEntity.ok(holding);
    }

    /**
     * 보유 종목 삭제
     */
    @DeleteMapping("/{portfolioId}/holdings/{holdingId}")
    public ResponseEntity<Void> deleteHolding(
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId,
            @AuthenticationPrincipal Long userId) {
        log.info("보유 종목 삭제 요청. 포트폴리오 ID: {}, 보유 종목 ID: {}", portfolioId, holdingId);
        
        portfolioService.deleteHolding(portfolioId, holdingId, userId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 포트폴리오 재계산 (현재 가격 반영)
     */
    @PostMapping("/{portfolioId}/recalculate")
    public ResponseEntity<PortfolioDto> recalculatePortfolio(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal Long userId) {
        log.info("포트폴리오 재계산 요청. 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);
        
        PortfolioDto portfolio = portfolioService.recalculatePortfolio(portfolioId, userId);
        
        return ResponseEntity.ok(portfolio);
    }
}
