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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 포트폴리오 엔티티
 * 사용자의 투자 포트폴리오를 관리하는 핵심 엔티티
 */
@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "total_gain_loss", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalGainLoss = BigDecimal.ZERO;

    @Column(name = "total_gain_loss_percent", precision = 8, scale = 4, nullable = false)
    private BigDecimal totalGainLossPercent = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PortfolioHolding> holdings = new ArrayList<>();

    @Builder
    public Portfolio(Long userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
    }

    /**
     * 포트폴리오 정보 업데이트
     */
    public void updatePortfolio(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 포트폴리오 총 가치 업데이트
     */
    public void updateTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
        calculateGainLoss();
    }

    /**
     * 포트폴리오 총 비용 업데이트
     */
    public void updateTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
        calculateGainLoss();
    }

    /**
     * 손익 계산
     */
    private void calculateGainLoss() {
        if (totalCost != null && totalValue != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            this.totalGainLoss = totalValue.subtract(totalCost);
            this.totalGainLossPercent = totalGainLoss
                    .divide(totalCost, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.totalGainLoss = BigDecimal.ZERO;
            this.totalGainLossPercent = BigDecimal.ZERO;
        }
    }

    /**
     * 보유 종목 추가
     */
    public void addHolding(PortfolioHolding holding) {
        holdings.add(holding);
        holding.setPortfolio(this);
    }

    /**
     * 보유 종목 제거
     */
    public void removeHolding(PortfolioHolding holding) {
        holdings.remove(holding);
        holding.setPortfolio(null);
    }

    /**
     * 포트폴리오 재계산
     */
    public void recalculate() {
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (PortfolioHolding holding : holdings) {
            if (holding.getMarketValue() != null) {
                totalValue = totalValue.add(holding.getMarketValue());
            }
            if (holding.getTotalCost() != null) {
                totalCost = totalCost.add(holding.getTotalCost());
            }
        }

        this.totalValue = totalValue;
        this.totalCost = totalCost;
        calculateGainLoss();
    }
}
