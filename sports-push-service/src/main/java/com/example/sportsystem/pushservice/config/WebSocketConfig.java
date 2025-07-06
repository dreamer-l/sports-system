package com.example.sportsystem.pushservice.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketRegistry;

/**
 * WebSocket 主配置类
 * 启用 WebSocket 并注册端点
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandshakeHandler handshakeHandler;

    public WebSocketConfig(WebSocketHandshakeHandler handshakeHandler) {
        this.handshakeHandler = handshakeHandler;
    }

    /**
     * 注册 WebSocket 端点并启用 STOMP（可选）
     * @param registry WebSocket 注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketRegistry registry) {
        registry.addHandler(handshakeHandler, "/ws")
                .addInterceptors(new OriginHandshakeInterceptor()) // 添加拦截器防止非法跨域访问
                .setAllowedOrigins("*"); // 可根据环境替换为白名单
    }

    /**
     * 自定义 Tomcat 配置，启用 SSL
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            factory.setProtocol("org.apache.coyote.http11.Http11NioProtocol");
            factory.addConnectorCustomizers(connector -> {
                connector.setSecure(true);
                connector.setScheme("https");
                connector.setAttribute("SSLEnabled", true);
                connector.setAttribute("keystoreFile", "classpath:keystore.jks");
                connector.setAttribute("keystorePass", "your-keystore-password");
                connector.setAttribute("keyAlias", "tomcat");
            });
        };
    }
}