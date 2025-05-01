package org.mjuecs.mjuecs.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

@Service
public class PortAllocator {

    private static final int START_PORT = 9000;
    private static final int END_PORT = 23000;
    private static final int MAX_RANDOM_ATTEMPTS = 300;
    private final Random random = new Random();

    /**
     * 먼저 랜덤 포트 검사 후, 실패 시 순차적 포트 검사로 전환하여 사용 가능한 포트를 할당합니다.
     * @return 사용 가능한 포트 번호 (없을 경우 -1 반환)
     */
    public synchronized int allocatePort() {
        // 1단계: 랜덤 포트 검사
        int attempt = 0;
        while (attempt++ < MAX_RANDOM_ATTEMPTS) {
            int port = START_PORT + random.nextInt(END_PORT - START_PORT + 1);

            try (ServerSocket ignored = new ServerSocket(port)) {
                ignored.setReuseAddress(true);
                return port; // 사용 가능한 포트 발견
            } catch (IOException e) {
                // 이미 사용 중인 포트 → 계속 시도
            }
        }

        // 2단계: 랜덤 실패 후 순차적 검사
        for (int port = START_PORT; port <= END_PORT; port++) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                ignored.setReuseAddress(true);
                return port; // 처음 발견한 사용 가능한 포트 반환
            } catch (IOException e) {
                // 사용 불가 포트 → 다음으로 이동
            }
        }

        // 3단계: 모든 포트가 사용 중일 때
        return -1;
    }
}