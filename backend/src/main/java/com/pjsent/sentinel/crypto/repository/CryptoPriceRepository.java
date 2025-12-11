package com.pjsent.sentinel.crypto.repository;

import com.pjsent.sentinel.crypto.entity.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, Long> {
}
