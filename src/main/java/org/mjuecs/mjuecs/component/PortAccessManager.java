package org.mjuecs.mjuecs.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PortAccessManager {
    private final Map<Integer, Instant> unlockedPorts = new ConcurrentHashMap<>();
    private static final long TIMEOUT_SECONDS = 180;

    public void allow(int port) {
        unlockedPorts.put(port, Instant.now());
        runCommand("iptables -D INPUT -p tcp --dport " + port + " -j DROP");
    }

    public void refresh(int port) {
        unlockedPorts.put(port, Instant.now());
    }

    @Scheduled(fixedRate = 30000)
    public void expireOldPorts() {
        Instant now = Instant.now();
        unlockedPorts.entrySet().removeIf(entry -> {
            boolean expired = now.minusSeconds(TIMEOUT_SECONDS).isAfter(entry.getValue());
            if (expired) {
                runCommand("iptables -A INPUT -p tcp --dport " + entry.getKey() + " -j DROP");
            }
            return expired;
        });
    }

    private void runCommand(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            throw new RuntimeException("iptables 명령 실패: " + cmd, e);
        }
    }

    public void block(int port) {
        runCommand("iptables -A INPUT -p tcp --dport " + port + " -j DROP");
    }
}