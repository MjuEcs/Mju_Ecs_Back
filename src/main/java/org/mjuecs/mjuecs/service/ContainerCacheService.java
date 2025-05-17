package org.mjuecs.mjuecs.service;

import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
public class ContainerCacheService {


    private final Map<String, ContainerStatusDto> statusCache = new ConcurrentHashMap<>();
    private final Map<String, String> ownershipCache = new ConcurrentHashMap<>();

    public void updateStatus(ContainerStatusDto newStatus, BiConsumer<String, ContainerStatusDto> onChanged) {
        String containerId = newStatus.getContainerId();
        ContainerStatusDto old = statusCache.get(containerId);

        boolean changed = (old == null)
                || !Objects.equals(old.getStatus(), newStatus.getStatus());

        if (changed) {
            statusCache.put(containerId, newStatus);
            String userId = ownershipCache.get(containerId);
            if (userId != null) {
                onChanged.accept(userId, newStatus);
            }
        }
    }

    public void bindOwner(String containerId, String userId) {
        ownershipCache.put(containerId, userId);
    }

    public void remove(String containerId) {
        statusCache.remove(containerId);
        ownershipCache.remove(containerId);
    }
}