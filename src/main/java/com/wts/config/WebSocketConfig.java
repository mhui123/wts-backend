// WebSocket/STOMP 구성: 클라이언트용 STOMP 엔드포인트와 간단한 메세지 브로커를 설정합니다.
// 주요 책임: /ws 엔드포인트 등록 및 애플리케이션 목적지/브로커 접두사 설정
package com.wts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    /*
    * /topic으로 시작하는 목적지에 대한 인메모리 메시지 브로커를 활성화
        클라이언트가 /topic/quotes 같은 주소를 구독할 수 있게 함
        애플리케이션 목적지 접두사:
        /app으로 시작하는 메시지는 **컨트롤러의 @MessageMapping**으로 라우팅
        클라이언트에서 서버로 메시지를 전송할 때 사용
    * */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    //구독한 클라이언트가 연결할 수 있는 STOMP 엔드포인트를 /ws로 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
