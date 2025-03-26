package org.mjuecs.mjuecs.controller;

import org.mjuecs.mjuecs.service.DockerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/docker")
public class DockerController {

    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @PostMapping("/run")
    public String runContainer(@RequestParam String image) {
        return dockerService.createAndStartContainer(image);
    }

    @DeleteMapping("/remove")
    public String removeContainer(@RequestParam String id) {
        dockerService.removeContainer(id);
        return "삭제 완료: " + id;
    }
}
