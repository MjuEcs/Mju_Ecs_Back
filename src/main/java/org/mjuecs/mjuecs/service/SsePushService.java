package org.mjuecs.mjuecs.service;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.controller.SseController;
import org.mjuecs.mjuecs.domain.ContainerStatus;
import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class SsePushService {

    private final SseController sseController;

    public void sendToUser(String userId, ContainerStatus status) {
        SseEmitter emitter = sseController.getEmitter(userId);
        if (emitter != null) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .data(toDto(status));
                emitter.send(event);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }

    private ContainerStatusDto toDto(ContainerStatus status) {
        return new ContainerStatusDto(
                status.getContainerId(),
                status.getStatus(),
                status.getImage(),
                status.getStartedAt(),
                status.getHostPort(),
                status.getContainerPort()
        );
    }
}
