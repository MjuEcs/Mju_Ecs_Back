package org.mjuecs.mjuecs.component;

import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
    }

    public void sendTo(String userId, ContainerStatusDto dto) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {

            try {
                emitter.send(dto);
            } catch (Exception e) {
                emitter.completeWithError(e);
                emitters.remove(userId);
            }
        }

    }

    public void remove(String userId) {
        emitters.remove(userId);
    }
}
