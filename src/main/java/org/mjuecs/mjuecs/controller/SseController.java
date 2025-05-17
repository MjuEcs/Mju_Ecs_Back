package org.mjuecs.mjuecs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.component.SseEmitterRegistry;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.mjuecs.mjuecs.service.ContainerCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {

    private final SseEmitterRegistry emitterRegistry;

    @GetMapping(value = "/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request) {
        String userId = extractUserIdFromToken(request);
        SseEmitter emitter = new SseEmitter(60000L);
        emitterRegistry.register(userId, emitter);

        emitter.onCompletion(() -> emitterRegistry.remove(userId));
        emitter.onTimeout(() -> emitterRegistry.remove(userId));

        return emitter;
    }

    private String extractUserIdFromToken(HttpServletRequest request) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return studentId;
//        String token = request.getHeader("Authorization");
//        if (token != null && token.equals("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdHVkZW50SWQiOiI2MDIwMjE3MyIsImlhdCI6MTc0NzQ0MjE5MCwiZXhwIjoxNzQ3NTI4NTkwfQ.GBDX7JNb_aC7PZUX_AIdUpa7D_2WTJrBxb_v27CMS48")) {
//            return "60202173"; // studentId 직접 리턴
//        }
//        return null;
    }
}
