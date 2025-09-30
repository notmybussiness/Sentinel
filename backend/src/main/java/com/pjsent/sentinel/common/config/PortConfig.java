package com.pjsent.sentinel.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

/**
 * 포트 자동 설정
 * 8080부터 시작해서 사용 가능한 포트를 자동으로 찾습니다.
 */
@Component
@Slf4j
public class PortConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        int port = findAvailablePort(8080, 8082);
        factory.setPort(port);
        log.info("🚀 애플리케이션이 포트 {}에서 시작됩니다.", port);
    }

    /**
     * 지정된 범위에서 사용 가능한 포트를 찾습니다.
     */
    private int findAvailablePort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        log.warn("⚠️  포트 {}-{} 범위에서 사용 가능한 포트가 없습니다. 기본 포트 {}를 사용합니다.",
                startPort, endPort, startPort);
        return startPort;
    }

    /**
     * 포트가 사용 가능한지 확인합니다.
     */
    private boolean isPortAvailable(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return false; // 연결 성공 시 포트가 사용 중
        } catch (IOException e) {
            return true; // 연결 실패 시 포트가 사용 가능
        }
    }
}