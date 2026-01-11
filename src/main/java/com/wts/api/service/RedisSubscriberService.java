package com.wts.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriberService implements MessageListener {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToChannels() {
        // 실시간 시세 채널 구독
        redisMessageListenerContainer.addMessageListener(
                this,
                new ChannelTopic("realtime_price_data")
        );

//        // 거래 데이터 채널 구독 (필요시)
//        redisMessageListenerContainer.addMessageListener(
//                this,
//                new ChannelTopic("trades_channel")
//        );

        log.info("Redis 채널 구독 시작: realtime_price_data");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.debug("Redis 메시지 수신 - 채널: {}, 내용: {}", channel, body);

            // 채널에 따른 처리 분기
            switch (channel) {
                case "realtime_price_data":
                    handleQuotesMessage(body);
                    break;
//                case "trades_channel":
//                    handleTradesMessage(body);
//                    break;
                default:
                    log.warn("알 수 없는 채널: {}", channel);
            }
        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 오류 발생", e);
        }
    }

    private void handleQuotesMessage(String messageBody) {
        try {
            // JSON 파싱 및 WebSocket으로 클라이언트에게 전송
            Object quotesData = objectMapper.readValue(messageBody, Object.class);

            // STOMP WebSocket을 통해 클라이언트에게 브로드캐스트
            // STOMP WebSocket을 통해 /topic/quotes 토픽을 구독하고 있는 모든 클라이언트들에게 전송
            messagingTemplate.convertAndSend("/topic/quotes", quotesData);

            log.debug("실시간 시세 데이터를 클라이언트에게 전송: {}", messageBody);
        } catch (Exception e) {
            log.error("시세 메시지 처리 중 오류", e);
        }
    }

    private void handleTradesMessage(String messageBody) {
        try {
            // 거래 데이터 처리 로직
            Object tradesData = objectMapper.readValue(messageBody, Object.class);

            // STOMP WebSocket을 통해 클라이언트에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/trades", tradesData);

            log.debug("거래 데이터를 클라이언트에게 전송: {}", messageBody);
        } catch (Exception e) {
            log.error("거래 메시지 처리 중 오류", e);
        }
    }
}
