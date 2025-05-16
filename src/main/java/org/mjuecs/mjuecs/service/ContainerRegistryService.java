package org.mjuecs.mjuecs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.mjuecs.mjuecs.repository.DockerContainerRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ContainerRegistryService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ContainerCacheService cache;
    private final DockerContainerRepository dockerContainerRepository;

    @Scheduled(fixedRate = 1000)

    public void collectStatuses() {
        try {
            Process process = new ProcessBuilder("sh", "-c",
                    "curl -s --unix-socket /var/run/docker.sock http://localhost/containers/json?all=1").start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            JsonNode root = objectMapper.readTree(reader);

            for (JsonNode container : root) {
                String containerId = container.get("Id").asText();
                String status = container.get("State").asText();
                String image = container.get("Image").asText();
                Instant startedAt = Instant.ofEpochSecond(container.get("Created").asLong());

//                JsonNode ports = container.get("Ports").get(0);
//                int hostPort = ports.get("PublicPort").asInt();
//                int containerPort = ports.get("PrivatePort").asInt();

                JsonNode names = container.get("Names");
                if (names != null && names.toString().contains("MjuEcs")) {
                    ContainerStatusDto statusDto = ContainerStatusDto.builder()
                            .containerId(containerId)
                            .status(status)
                            .image(image)
                            .startedAt(startedAt.toString())
                            .build();
                    cache.putStatus(statusDto);

                    if (names.size() > 0) {
                        String rawName = names.get(0).asText();
                        String[] parts = rawName.split("-");
                        if (parts.length > 1) {
                            String userId = parts[1].replaceAll("[^a-zA-Z0-9]", "");
                            cache.bindOwner(containerId, userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}