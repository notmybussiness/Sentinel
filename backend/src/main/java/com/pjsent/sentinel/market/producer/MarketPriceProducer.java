package com.pjsent.sentinel.market.producer;

import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketPriceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "market-price-updates";

    public void publishPriceUpdate(PriceUpdateEvent event) {
        log.debug("Publishing price update event: {}", event);
        kafkaTemplate.send(TOPIC, event.symbol(), event);
    }
}
