package com.pjsent.sentinel.market.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "stock.market")
public class StockMarketProperties {

    private KoreaInvestment koreaInvestment = new KoreaInvestment();
    private Provider alphavantage = new Provider();
    private Provider finnhub = new Provider();
    private Yahoo yahoo = new Yahoo();

    @Getter
    @Setter
    public static class Provider {
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private int timeout = 10000;
        private int rateLimit = 0;
    }

    @Getter
    @Setter
    public static class KoreaInvestment {
        private boolean enabled = true;
        private String baseUrl;
        private String appKey;
        private String appSecret;
        private int timeout = 10000;
        private long tokenExpiration = 86400;
        private long tokenRefreshBefore = 3600;
    }

    @Getter
    @Setter
    public static class Yahoo {
        private boolean enabled = false;
        private boolean devOnly = true;
    }
}
