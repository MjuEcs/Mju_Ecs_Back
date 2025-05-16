package org.mjuecs.mjuecs.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ContainerOwnerService {

    private final Map<String, String> ownership = Map.of(
            "containerId1", "user1",
            "containerId2", "user2"
    );

    public String findOwnerIdByContainerId(String containerId) {
        return ownership.get(containerId);
    }
}

