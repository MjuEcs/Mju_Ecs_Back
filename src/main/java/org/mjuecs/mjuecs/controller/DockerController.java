package org.mjuecs.mjuecs.controller;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.mjuecs.mjuecs.service.DockerService;
import org.mjuecs.mjuecs.service.PortAllocator;
import org.mjuecs.mjuecs.service.TtydService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.mjuecs.mjuecs.annotation.RateLimit;

import java.util.Optional;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerService dockerService;
    private final StudentRepository studentRepository;
    private final TtydService ttydLauncherService;
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

//        // 2. ttyd 실행 (내부에서 임의 포트 할당 수행)
//        String ttydUrl = ttydLauncherService.launchTtydForContainer(containerId);
//        if (ttydUrl == null) {
//            return ResponseEntity.status(500).body("ttyd 실행 실패");
//        }

        // 3. 성공 시 URL 반환
        return ResponseEntity.ok(
                "containerId: " + containerId + ", ttydUrl: " + "not local (test)");
    }

    /**
     * 컨테이너 상태 조회 API입니다. 현재는 단순 HTTP1.1 방식으로 구현되어 있습니다
     * - containerId 파라미터가 있을 경우 해당 컨테이너의 상태를 반환합니다.
     * - 없을 경우 로그인한 사용자의 모든 컨테이너 상태를 반환합니다.
     *
     * @param containerId (optional) 특정 컨테이너 ID
     * @return 컨테이너 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<?> getContainerStatus(
            @RequestParam(name = "containerId", required = false) String containerId) {

        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);

        if (containerId != null && !containerId.isBlank()) {
            return dockerService.getContainerStatus(containerId, student.get());
        } else {
            return dockerService.getAllContainerStatuses(student.get());
        }
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

    @GetMapping("/download")
    @RateLimit(limit = 1, period = 30) // 30초에 최대 1회 요청 제한
    public ResponseEntity<FileSystemResource> downloadContainerFiles(@RequestParam("containerId") String containerId) {
        try {
            File zipFile = dockerService.downloadContainerFiles(containerId);

            FileSystemResource resource = new FileSystemResource(zipFile);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=container-files.zip");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipFile.length())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
