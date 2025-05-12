package org.mjuecs.mjuecs.controller;

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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class TerminalController {
    private final DockerContainerRepository dockerContainerRepository;
    private final PortAccessManager portAccessManager;
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
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
