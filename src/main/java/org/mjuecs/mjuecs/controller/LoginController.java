package org.mjuecs.mjuecs.controller;

import com.github.dockerjava.api.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.dto.LoginDto;
import org.mjuecs.mjuecs.service.StudentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final StudentService studentService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        try {
            String token = studentService.login(loginDto);

            Cookie cookie = new Cookie("token", token);
            cookie.setMaxAge(86400000);
            cookie.setHttpOnly(true);

//            return ResponseEntity.ok(Map.of("token", "Bearer " + token)).header(HttpHeaders.SET_COOKIE, cookie.toString());
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(Map.of("token", "Bearer " + token));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패");
        }
    }
}
