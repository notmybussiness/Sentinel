package com.pjsent.sentinel.pricehistory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가격 이력 엔티티
 *
 * 주식 및 암호화폐의 시간별 가격 데이터 저장
 * OHLCV (Open, High, Low, Close, Volume) 데이터 포함
 */
@Entity
@Table(
    name = "price_history",
    indexes = {
        @Index(name = "idx_symbol_timestamp", columnList = "symbol,timestamp"),
        @Index(name = "idx_asset_type_timestamp", columnList = "asset_type,timestamp"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 심볼 (AAPL, GOOGL, BTC, ETH 등)
     */
    @Column(nullable = false, length = 20)
    private String symbol;

    /**
     * 자산 유형 (STOCK, CRYPTO, ETF, INDEX)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    private AssetType assetType;

    /**
     * 기준 통화 (USD, KRW 등)
     */
    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    /**
     * 타임스탬프
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * 시가 (Open)
     */
    @Column(precision = 20, scale = 8)
    private BigDecimal open;

    /**
     * 고가 (High)
     */
    @Column(precision = 20, scale = 8)
    private BigDecimal high;

    /**
     * 저가 (Low)
     */
    @Column(precision = 20, scale = 8)
    private BigDecimal low;

    /**
     * 종가 (Close)
     */
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal close;

    /**
     * 거래량 (Volume)
     */
    @Column(precision = 20, scale = 2)
    private BigDecimal volume;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 자산 유형 Enum
     */
    public enum AssetType {
        STOCK,   // 주식
        CRYPTO,  // 암호화폐
        ETF,     // ETF
        INDEX    // 지수 (S&P500, NASDAQ 등)
    }
}
