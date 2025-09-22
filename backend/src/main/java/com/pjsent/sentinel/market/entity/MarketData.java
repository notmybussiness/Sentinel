package com.pjsent.sentinel.market.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 시장 데이터 엔티티
 * 외부 API에서 조회한 시장 데이터를 캐싱하기 위한 엔티티
 */
@Entity
@Table(name = "market_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "price", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "change_percent", precision = 8, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "high_24h", precision = 15, scale = 4)
    private BigDecimal high24h;

    @Column(name = "low_24h", precision = 15, scale = 4)
    private BigDecimal low24h;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Builder
    public MarketData(String symbol, BigDecimal price, Long volume, BigDecimal changePercent,
                     BigDecimal high24h, BigDecimal low24h, Long marketCap, String dataSource) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.changePercent = changePercent;
        this.high24h = high24h;
        this.low24h = low24h;
        this.marketCap = marketCap;
        this.dataSource = dataSource;
    }

    /**
     * 시장 데이터 업데이트
     */
    public void updateMarketData(BigDecimal price, Long volume, BigDecimal changePercent,
                               BigDecimal high24h, BigDecimal low24h, Long marketCap) {
        this.price = price;
        this.volume = volume;
        this.changePercent = changePercent;
        this.high24h = high24h;
        this.low24h = low24h;
        this.marketCap = marketCap;
    }
}
