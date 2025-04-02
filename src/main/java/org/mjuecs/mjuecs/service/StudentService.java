package org.mjuecs.mjuecs.service;

import com.github.dockerjava.api.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.repository.StudentRepository;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.LoginDto;
import org.mjuecs.mjuecs.jwt.JwtUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final JwtUtil jwtToken;
    private final RestTemplate restTemplate = new RestTemplate();

    public String login(LoginDto dto) {
        // 1. 외부 인증 요청
        boolean isVerified = verifyWithExternalAuthServer(dto);
        if (!isVerified) {
            throw new UnauthorizedException("외부 인증 실패");
        }
        // 2. 사용자 정보 DB 저장 (없을 경우만)
        Student loggedIn = studentRepository.findById(dto.getStudentId())
                .orElseGet(() -> {
                    Student student = new Student();
                    student.setStudentId(dto.getStudentId());
                    student.setName(dto.getName());
                    student.setLastLogin(new Date());
                    return studentRepository.save(student);
                });
        loggedIn.setLastLogin(new Date());
        studentRepository.save(loggedIn);

        // 3. JWT 발급
        return jwtToken.createJwt(dto.getStudentId(), dto.getName(), 86400000L);//하루치 토큰 기간
    }

    private boolean verifyWithExternalAuthServer(LoginDto dto) {
        String url = "https://sso1.mju.ac.kr/mju/userCheck.do";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", dto.getStudentId());
        formData.add("passwrd", dto.getPasswrd());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            String errorCode = (String) response.getBody().get("error");
            return "0000".equals(errorCode);
        } catch (Exception e) {
            return false;
        }
    }
}
