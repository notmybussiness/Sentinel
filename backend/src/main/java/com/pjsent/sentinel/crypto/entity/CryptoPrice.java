package com.pjsent.sentinel.crypto.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_price_history", indexes = {
        @Index(name = "idx_crypto_symbol_timestamp", columnList = "symbol, timestamp")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CryptoPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol; // e.g., "BTC", "ETH"

    @Column(nullable = false)
    private String currency; // e.g., "KRW"

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Builder
    public CryptoPrice(String symbol, String currency, BigDecimal price, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.currency = currency;
        this.price = price;
        this.timestamp = timestamp;
    }
}
