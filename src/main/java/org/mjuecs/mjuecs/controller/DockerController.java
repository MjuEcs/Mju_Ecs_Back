package org.mjuecs.mjuecs.controller;

import java.util.Optional;

import org.mjuecs.mjuecs.Repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.RunContainerRequestDto;
import org.mjuecs.mjuecs.exception.StudentNotFoundException;
import org.mjuecs.mjuecs.service.DockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerService dockerService;
    private final StudentRepository studentRepository;


    @PostMapping("/run")
    public ResponseEntity<String> runCustomContainer(@RequestBody RunContainerRequestDto dto) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다"));

        String containerId = dockerService.createCustomContainer(dto, student);
        return ResponseEntity.ok(containerId);
    }

    @DeleteMapping("/remove")
    public String removeContainer(@RequestParam("id") String id) {
        dockerService.removeContainer(id);
        return "삭제 완료: " + id;
    }
}
