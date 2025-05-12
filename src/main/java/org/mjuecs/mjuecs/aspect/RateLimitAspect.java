package org.mjuecs.mjuecs.aspect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.mjuecs.mjuecs.annotation.RateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RateLimitAspect {

    private final Map<String, RequestInfo> requestMap = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();

        RequestInfo info = requestMap.getOrDefault(key, new RequestInfo(0, 0));

        if (now - info.firstRequestTime > rateLimit.period() * 1000L) {
            info = new RequestInfo(1, now);
            requestMap.put(key, info);
        } else {
            info.count++;
            if (info.count > rateLimit.limit()) {
                return ResponseEntity.status(429).body("Too Many Requests");
            }
            requestMap.put(key, info);
        }

        return joinPoint.proceed();
    }

    private static class RequestInfo {
        int count;
        long firstRequestTime;

        public RequestInfo(int count, long firstRequestTime) {
            this.count = count;
            this.firstRequestTime = firstRequestTime;
        }
    }
}
