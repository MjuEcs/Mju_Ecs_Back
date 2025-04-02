package org.mjuecs.mjuecs.controller;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.mjuecs.mjuecs.service.DockerService;
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
        return dockerService.createCustomContainer(dto,student.get());
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeContainer(@RequestParam String id) {
        return dockerService.removeContainer(id);
    }
}
