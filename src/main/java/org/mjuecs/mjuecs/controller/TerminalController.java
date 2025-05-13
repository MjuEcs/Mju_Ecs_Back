package org.mjuecs.mjuecs.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.component.PortAccessManager;
import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.repository.DockerContainerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class TerminalController {
    private final DockerContainerRepository dockerContainerRepository;
    private final PortAccessManager portAccessManager;
    private final DockerClient dockerClient = DockerClientBuilder.getInstance().build();
    @PostMapping("/unlock/{containerId}")
    public ResponseEntity<?> unlock(@PathVariable String containerId) {
        return dockerContainerRepository.findById(containerId)
                .map(container -> {
                    portAccessManager.allow(container.getTtydHostPort());
                    return ResponseEntity.ok(Map.of(
                            "url", "http://localhost:" + container.getTtydHostPort()
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/ping/{containerId}")
    public ResponseEntity<?> ping(@PathVariable String containerId) {
        return dockerContainerRepository.findById(containerId)
                .map(container -> {
                    portAccessManager.refresh(container.getTtydHostPort());

                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "pong");

                    try {
                        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
                        response.put("status", containerInfo.getState().getStatus());
                        response.put("startedAt", containerInfo.getState().getStartedAt());
                        response.put("image", container.getImage());
                        response.put("ports", Map.of(
                                "hostPort", container.getHostPort(),
                                "containerPort", container.getContainerPort()
                        ));
                    } catch (Exception e) {
                        response.put("statusError", e.getMessage());
                    }

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
