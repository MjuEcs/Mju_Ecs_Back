package org.mjuecs.mjuecs.controller;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.mjuecs.mjuecs.service.DockerService;
import org.mjuecs.mjuecs.service.TtydLauncherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerService dockerService;
    private final StudentRepository studentRepository;
    private final TtydLauncherService ttydLauncherService;

    private static int basePort = 9000;
    private static int currentOffset = 0;

    @PostMapping("/run")
    public ResponseEntity<?> runContainer(@RequestParam String image) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);
        return dockerService.createContainer(image,student.get());
    }

    @PostMapping("/custom/run")
    public ResponseEntity<?> runCustomContainer(@RequestBody ContainerDto dto) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);

        String containerId = dockerService.createCustomContainer(dto, student.get());

        int port = basePort + (currentOffset++ % 1000);
        if(containerId!="컨테이너 생성 불가") {
            boolean success = ttydLauncherService.launchTtydForContainer(containerId, port);
            if (success) {
                return ResponseEntity.ok("http://localhost:" + port);
            } else {
                return ResponseEntity.status(500).body("ttyd 실행 실패");
            }
        }
        return ResponseEntity.status(500).body("컨테이너 생성 실패");
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeContainer(@RequestParam String id) {
        return dockerService.removeContainer(id);
    }
}
