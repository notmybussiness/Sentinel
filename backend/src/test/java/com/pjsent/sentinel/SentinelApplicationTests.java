package com.pjsent.sentinel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "stock.market.alphavantage.api-key=test-key",
    "stock.market.finnhub.api-key=test-key"
})
class SentinelApplicationTests {

	@Test
	void contextLoads() {
	}

}
