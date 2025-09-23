package com.pjsent.sentinel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "stock.market.alphavantage.api-key=test-key",
    "stock.market.finnhub.api-key=test-key",
    "jwt.secret=test-jwt-secret-key-for-testing-only",
    "kakao.oauth.client-id=test-kakao-client-id",
    "kakao.oauth.client-secret=test-kakao-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/api/v1/auth/kakao/callback"
})
class SentinelApplicationTests {

	@Test
	void contextLoads() {
	}

}
