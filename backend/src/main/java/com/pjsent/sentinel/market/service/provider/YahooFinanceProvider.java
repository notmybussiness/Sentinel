package com.pjsent.sentinel.market.service.provider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.market.dto.StockPriceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Yahoo Finance API ????썹땟怨⒲뀋?????낆뵒???
 * ???癲????類ㅺ퉻???API???????????⑤베鍮?????녿뮝????????????? ???곌떽釉붾??嶺뚮ㅎ????
 * 15-20???꿔꺂???????살퓢??룐뫁???됱삩??????繹먮냱?????쇨덫??????뚭퐫??汝??吏??노????嶺뚮??ｆ쾮???꿔꺂?????沃섃뫂???????딅젩.
 */
@Component
@Order(3) // 3rd priority fallback
@Profile("dev")
@Slf4j
public class YahooFinanceProvider implements MarketDataProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Yahoo Finance ????썹땟??RestTemplate??ObjectMapper?????녿뮝????????놃닓 ???꾩룆????
    public YahooFinanceProvider(
            @Qualifier("yahooRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Yahoo Finance Chart API ???됰Ŧ???????
    private static final String YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";

    // ??嶺뚮쮳?놂폇????醫딆쓧????꾩룆????꿔꺂?????용Ъ????????????
    private static final String HEALTH_CHECK_SYMBOL = "AAPL";

    @Override
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("Yahoo Finance API??醫딆쓧? ???????곗뵯???嚥싳쇎紐???????딅젩.");
        }

        log.info("Fetch market data from Yahoo Finance. symbol={}", symbol);

        try {
            String url = buildChartUrl(symbol);
            log.debug("Yahoo Finance API ?癲ル슢????URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseYahooResponse(symbol, response.getBody());
            } else {
                log.warn("Yahoo Finance API ????????????????쇨덫??????딅젩. ????븐뻤???ш끽維??? {}", response.getStatusCode());
                throw new RuntimeException("Yahoo Finance API ???????????怨몄뵒");
            }

        } catch (Exception e) {
            log.error("Yahoo Finance API ?癲ル슢????嚥?????怨몄뵒 ?熬곣뫖利든뜏類ｋ렱?? ???? {}, ????怨몄뵒: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Yahoo Finance API ?癲ル슢?????????곌숯: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // ??醫딆┣????????筌뚮벉???ш낄猷??- Apple ???녿뮝??????Β????????
            String testUrl = buildChartUrl(HEALTH_CHECK_SYMBOL);
            ResponseEntity<String> response = restTemplate.getForEntity(testUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // ????????chart ?????????? ?????됲닓?꿔꺂??? ?癲ル슢캉????
                return response.getBody().contains("\"chart\"") &&
                       response.getBody().contains("\"result\"");
            }
            return false;

        } catch (Exception e) {
            log.warn("Yahoo Finance ??嶺뚮쮳?놂폇????醫딆쓧????꾩룆????꿔꺂?????용Ъ??????곌숯: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Yahoo Finance";
    }

    @Override
    public boolean supportsTimeSeries() {
        return true;
    }

    @Override
    public boolean supportsHistoricalData() {
        return true;
    }

    @Override
    public boolean supportsSymbol(String symbol) {
        return true;
    }

    /**
     * ?????Yahoo Finance ?癲ル슢캉??쏆춿????Β????⑤슢堉???
     */
    private String convertToYahooSymbol(String symbol) {
        // ???곌떽?댁맾 ???녿뮝??? 6??????????(?? 005930 ??005930.KS)
        if (symbol.matches("\\d{6}")) {
            return symbol + ".KS";
        }
        // ????ㅻ쿅?????녿뮝??? 4??????????(?? 7203 ??7203.T)
        if (symbol.matches("\\d{4}")) {
            return symbol + ".T";
        }
        // ???붺몭??????녿뮝????????뚯???: ???녾컯嶺???????
        return symbol;
    }

    /**
     * Yahoo Finance Chart API URL ???????
     */
    private String buildChartUrl(String symbol) {
        // ?????Yahoo Finance ?癲ル슢캉??쏆춿????Β????⑤슢堉???
        String yahooSymbol = convertToYahooSymbol(symbol);
        // ???뚯???????쇨덫嶺뚮ㅏ諭??1?????????????산뭐勇??꿔꺂????쭍????醫딆쓧????癲ル슢???ъ쒜????醫딆쓧??癲ル슢???몃Ь?
        return YAHOO_FINANCE_BASE_URL + yahooSymbol + "?interval=1d&range=1d";
    }

    /**
     * Yahoo Finance ????????StockPriceDto????⑤슢堉???
     */
    private StockPriceDto parseYahooResponse(String symbol, String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // chart.result[0] ?嚥▲굧???뚪뜮??용뿫???????????????뗫떔??
            JsonNode chartNode = rootNode.path("chart");
            if (chartNode.isMissingNode()) {
                throw new RuntimeException("Yahoo Finance ???????????chart ?????????? ?꿔꺂????????????ㅿ폍??????딅젩.");
            }

            JsonNode resultArray = chartNode.path("result");
            if (resultArray.isMissingNode() || !resultArray.isArray() || resultArray.size() == 0) {
                throw new RuntimeException("Yahoo Finance ???????????result ?????????? ?꿔꺂????????????ㅿ폍??????딅젩.");
            }

            JsonNode resultNode = resultArray.get(0);

            // ?꿔꺂???? ??????????????援??????썹땟????醫딆쓧????癲ル슢???ъ쒜????ㅻ쿋驪??
            JsonNode metaNode = resultNode.path("meta");
            if (metaNode.isMissingNode()) {
                throw new RuntimeException("Yahoo Finance ???????????meta ?????????? ?꿔꺂????????????ㅿ폍??????딅젩.");
            }

            // ????썹땟????醫딆쓧??嚥▲굥?멩납??????ㅼ굣??????띻틯?
            double currentPrice = metaNode.path("regularMarketPrice").asDouble(0.0);
            // Yahoo Finance??????????썹땟???노???彛??됯샵????????????繹먮굞??
            double previousClose = metaNode.path("previousClose").asDouble(0.0);
            if (previousClose <= 0) {
                previousClose = metaNode.path("chartPreviousClose").asDouble(0.0);
            }
            if (previousClose <= 0) {
                previousClose = metaNode.path("regularMarketPreviousClose").asDouble(0.0);
            }
            log.debug("Yahoo Finance meta ????? ???? {}, currentPrice: {}, previousClose: {}", symbol, currentPrice, previousClose);

            if (currentPrice <= 0) {
                throw new RuntimeException("????ъ군???? ??? ??醫딆쓧???????????? " + currentPrice);
            }

            // OHLC ????????????ㅻ쿋驪??(?꿔꺂????쭍???????????
            JsonNode timestampArray = resultNode.path("timestamp");
            JsonNode indicatorsNode = resultNode.path("indicators");
            JsonNode quoteNode = indicatorsNode.path("quote").get(0);

            double open = 0.0, high = 0.0, low = 0.0, close = 0.0;
            LocalDateTime lastTradingDateTime = LocalDateTime.now();

            if (!timestampArray.isMissingNode() && timestampArray.isArray() && timestampArray.size() > 0) {
                int lastIndex = timestampArray.size() - 1;

                // ??????썹땟戮ъ쭍????
                long timestamp = timestampArray.get(lastIndex).asLong();
                lastTradingDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()
                );

                // OHLC ?????????
                if (!quoteNode.isMissingNode()) {
                    JsonNode openArray = quoteNode.path("open");
                    JsonNode highArray = quoteNode.path("high");
                    JsonNode lowArray = quoteNode.path("low");
                    JsonNode closeArray = quoteNode.path("close");

                    if (openArray.isArray() && lastIndex < openArray.size()) {
                        open = openArray.get(lastIndex).asDouble(0.0);
                    }
                    if (highArray.isArray() && lastIndex < highArray.size()) {
                        high = highArray.get(lastIndex).asDouble(0.0);
                    }
                    if (lowArray.isArray() && lastIndex < lowArray.size()) {
                        low = lowArray.get(lastIndex).asDouble(0.0);
                    }
                    if (closeArray.isArray() && lastIndex < closeArray.size()) {
                        close = closeArray.get(lastIndex).asDouble(0.0);
                    }
                }
            }

            // ??⑤슢堉????됰Ŧ?????影??낟??
            double change = currentPrice - previousClose;
            double changePercent = previousClose > 0 ? (change / previousClose) * 100 : 0.0;

            log.debug("Yahoo Finance ??????????????????썹땟?? ???? {}, ??醫딆쓧??? ${}, ??⑤슢堉??? {}%",
                     symbol, currentPrice, String.format("%.2f", changePercent));

            return StockPriceDto.builder()
                    .symbol(symbol)
                    .price(currentPrice)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .change(change)
                    .changePercent(changePercent)
                    .lastTradingDay(lastTradingDateTime.toLocalDate().toString())
                    .timeStamp(lastTradingDateTime)
                    .provider(getProviderName())
                    .build();

        } catch (Exception e) {
            log.error("Yahoo Finance ????????????嚥?????怨몄뵒 ?熬곣뫖利든뜏類ｋ렱?? ???? {}, ????怨몄뵒: {}",
                     symbol, e.getMessage(), e);
            throw new RuntimeException("Yahoo Finance ??????????????????????????곌숯: " + e.getMessage(), e);
        }
    }
}
