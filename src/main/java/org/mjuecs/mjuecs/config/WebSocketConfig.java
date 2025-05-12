// WebSocketBridgeConfig.java
package org.mjuecs.mjuecs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

//    private final TerminalProcessBridge terminalProcessBridge;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 등록 필요 시 작성 (현재는 @ServerEndpoint 사용 중)
    }
}
