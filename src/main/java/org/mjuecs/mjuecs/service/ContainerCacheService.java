package org.mjuecs.mjuecs.service;

import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ContainerCacheService {

    private final Map<String, ContainerStatusDto> statusCache = new ConcurrentHashMap<>();
    private final Map<String, String> ownershipCache = new ConcurrentHashMap<>();

    public void putStatus(ContainerStatusDto status) {
        statusCache.put(status.getContainerId(), status);
    }

    public void bindOwner(String containerId, String userId) {
        ownershipCache.put(containerId, userId);
    }

    public String getOwner(String containerId) {
        return ownershipCache.get(containerId);
    }

    public List<ContainerStatusDto> getAllOwnedBy(String userId) {
        return ownershipCache.entrySet().stream()
                .filter(e -> userId.equals(e.getValue()))
                .map(e -> statusCache.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void remove(String containerId) {
        statusCache.remove(containerId);
        ownershipCache.remove(containerId);
    }
}