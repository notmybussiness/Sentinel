package com.pjsent.sentinel.portfolio.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포트폴리오 보유 종목 엔티티
 * 포트폴리오 내 개별 종목의 보유 정보를 관리
 */
@Entity
@Table(name = "portfolio_holdings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "symbol"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "quantity", precision = 15, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(name = "average_cost", precision = 15, scale = 4, nullable = false)
    private BigDecimal averageCost;

    @Column(name = "current_price", precision = 15, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "market_value", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "gain_loss", precision = 15, scale = 2)
    private BigDecimal gainLoss;

    @Column(name = "gain_loss_percent", precision = 8, scale = 4)
    private BigDecimal gainLossPercent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PortfolioHolding(Portfolio portfolio, String symbol, BigDecimal quantity, BigDecimal averageCost) {
        this.portfolio = portfolio;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.totalCost = quantity.multiply(averageCost);
    }

    /**
     * 현재 가격 업데이트 및 손익 재계산
     */
    public void updateCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        if (currentPrice != null && quantity != null) {
            this.marketValue = quantity.multiply(currentPrice);
            calculateGainLoss();
        }
    }

    /**
     * 수량 업데이트
     */
    public void updateQuantity(BigDecimal newQuantity) {
        this.quantity = newQuantity;
        this.totalCost = quantity.multiply(averageCost);
        if (currentPrice != null) {
            this.marketValue = quantity.multiply(currentPrice);
            calculateGainLoss();
        }
    }

    /**
     * 평균 단가 업데이트
     */
    public void updateAverageCost(BigDecimal newAverageCost) {
        this.averageCost = newAverageCost;
        this.totalCost = quantity.multiply(averageCost);
        calculateGainLoss();
    }

    /**
     * 손익 계산
     */
    private void calculateGainLoss() {
        if (marketValue != null && totalCost != null) {
            this.gainLoss = marketValue.subtract(totalCost);
            if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
                this.gainLossPercent = gainLoss
                        .divide(totalCost, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                this.gainLossPercent = BigDecimal.ZERO;
            }
        } else {
            this.gainLoss = BigDecimal.ZERO;
            this.gainLossPercent = BigDecimal.ZERO;
        }
    }

    /**
     * 포트폴리오 설정 (양방향 관계 설정용)
     */
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    /**
     * 보유 종목 정보 업데이트 (수량과 평균 단가를 함께 업데이트)
     */
    public void updateHolding(BigDecimal newQuantity, BigDecimal newAverageCost) {
        this.quantity = newQuantity;
        this.averageCost = newAverageCost;
        this.totalCost = quantity.multiply(averageCost);
        if (currentPrice != null) {
            this.marketValue = quantity.multiply(currentPrice);
            calculateGainLoss();
        }
    }
}
