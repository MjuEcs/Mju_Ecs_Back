package org.mjuecs.mjuecs.component;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.domain.ContainerStatus;
import org.mjuecs.mjuecs.service.ContainerOwnerService;
import org.mjuecs.mjuecs.service.ContainerStatusCache;
import org.mjuecs.mjuecs.service.SsePushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ContainerPollScheduler {
    private final ContainerStatusCache cache;
    private final SsePushService ssePushService;
    private final ContainerOwnerService ownerService;

    @Scheduled(fixedRate = 1000)
    public void poll() {
        List<ContainerStatus> latest = loadCurrentStatuses();
        cache.update(latest, changedStatus -> {
            String ownerId = ownerService.findOwnerIdByContainerId(changedStatus.getContainerId());
            ssePushService.sendToUser(ownerId, changedStatus);
        });
    }

    private List<ContainerStatus> loadCurrentStatuses() {
        List<ContainerStatus> result = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("sh", "-c",
                    "curl -s --unix-socket /var/run/docker.sock http://localhost/containers/json?all=1").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                // JSON 파싱하여 result에 추가 (생략됨)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}