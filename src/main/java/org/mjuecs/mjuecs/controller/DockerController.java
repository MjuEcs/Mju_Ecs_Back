package org.mjuecs.mjuecs.controller;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.mjuecs.mjuecs.service.DockerService;
import org.mjuecs.mjuecs.service.PortAllocator;
import org.mjuecs.mjuecs.service.TtydLauncherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerService dockerService;
    private final StudentRepository studentRepository;
    private final TtydLauncherService ttydLauncherService;
    private final PortAllocator portAllocator;

    // @PostMapping("/run")
    // public ResponseEntity<?> runContainer(@RequestParam String image) {
    // String studentId = (String)
    // SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    // Optional<Student> student = studentRepository.findById(studentId);
    // return dockerService.createContainer(image,student.get());
    // }

    // @PostMapping("/custom/run")

    @PostMapping("/custom/run")
    public ResponseEntity<?> runCustomContainer(@RequestBody ContainerDto dto) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("학생 정보를 찾을 수 없습니다."));

        // 1. 컨테이너 생성 (내부에서 임의 포트 할당 수행)
        String containerId = dockerService.createCustomContainer(dto, student);
        if ("컨테이너 생성 불가".equals(containerId)) {
            return ResponseEntity.status(500).body("컨테이너 생성 실패");
        }

        // 2. ttyd 실행 (내부에서 임의 포트 할당 수행)
        String ttydUrl = ttydLauncherService.launchTtydForContainer(containerId);
        if (ttydUrl == null) {
            return ResponseEntity.status(500).body("ttyd 실행 실패");
        }

        // 3. 성공 시 URL 반환
        return ResponseEntity.ok(ttydUrl);
    }

    @PostMapping("/start")
    public ResponseEntity<?> startContainer(@RequestParam("containerId") String containerId) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);

        return dockerService.startContainer(containerId, student.get());
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopContainer(@RequestParam("containerId") String containerId) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);

        return dockerService.stopContainer(containerId, student.get());
    }

    @PostMapping("/restart")
    public ResponseEntity<?> restartContainer(@RequestParam("containerId") String containerId) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);

        return dockerService.restartContainer(containerId, student.get());
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeContainer(@RequestParam("containerId") String containerId) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);

        return dockerService.removeContainer(containerId, student.get());
    }
}
