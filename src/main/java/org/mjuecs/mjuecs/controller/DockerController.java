package org.mjuecs.mjuecs.controller;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.Repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.service.DockerService;
import org.springframework.security.core.context.SecurityContext;
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
    public String runContainer(@RequestParam String image) {
        String studentId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Student> student = studentRepository.findById(studentId);
        return dockerService.createAndStartContainer(image,student.get());
    }

    @DeleteMapping("/remove")
    public String removeContainer(@RequestParam String id) {
        dockerService.removeContainer(id);
        return "삭제 완료: " + id;
    }
}
