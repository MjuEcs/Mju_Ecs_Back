package org.mjuecs.mjuecs.service;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.domain.ContainerStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ContainerStatusCache {
    private final Map<String, ContainerStatus> cache = new ConcurrentHashMap<>();

    public void update(List<ContainerStatus> latest, Consumer<ContainerStatus> onChanged) {
        for (ContainerStatus now : latest) {
            ContainerStatus old = cache.get(now.getContainerId());
            if (old == null || !old.equals(now)) {
                cache.put(now.getContainerId(), now);
                onChanged.accept(now);
            }
        }
    }
}