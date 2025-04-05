package org.mjuecs.mjuecs.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.mjuecs.mjuecs.repository.DockerContainerRepository;
import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final DockerContainerRepository dockerContainerRepository;

    public DockerService(DockerContainerRepository dockerContainerRepository) {
        this.dockerContainerRepository = dockerContainerRepository;
        this.dockerClient = DockerClientFactory.createClient();
    }

    // 1. 이미지 존재 여부 체크 후 없으면 pull
    private void pullImageIfNotExists(String imageName) {
        try {
            dockerClient.inspectImageCmd(imageName).exec();
        } catch (NotFoundException e) {
            System.out.println("이미지가 없어서 pull 중...");
            try {
                dockerClient.pullImageCmd(imageName)
                        .start()
                        .awaitCompletion(); // ✅ pull 완료 대기!
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("이미지 pull 중 인터럽트 발생", ie);
            }
        }
    }


public String createCustomContainer(ContainerDto dto, Student student) {
    if (dockerContainerRepository.findByStudent(student).size() < 2) {
        pullImageIfNotExists(dto.getImageName());

        CreateContainerResponse container = dockerClient.createContainerCmd(dto.getImageName())
                .withName("MjuEcs-" + student.getStudentId() + "-" + System.currentTimeMillis())
                .withEnv(dto.getEnv().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue()).toList())
                .withHostConfig(HostConfig.newHostConfig())
                .withCmd(dto.getCmd())
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .withStdinOpen(true)
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        DockerContainer dockerContainer = new DockerContainer();
            dockerContainer.setContainerId(container.getId());
            dockerContainer.setImage(dto.getImageName());
            dockerContainer.setStudent(student);
            dockerContainerRepository.save(dockerContainer);


        return container.getId(); // ✅ 사용자에게 URL 제공
    }
    return "컨테이너 생성 불가";
}


    // 3. 컨테이너 삭제 (중지 → 삭제)
    public ResponseEntity<?> removeContainer(String containerId) {
        Optional<DockerContainer> dockerContainer = dockerContainerRepository.findById(containerId);
        //데이터 베이스 확인하고 컨테이너 아이디 있으면 삭제하고
        if(dockerContainer.isPresent()){
            //데이터 베이스 확인하고 컨테이너 아이디 있으면 삭제하고
            try {
                dockerClient.stopContainerCmd(containerId).exec();// 실패해도 넘어감
            } catch (NotModifiedException e) {
                // 이미 중지된 상태, 무시
            }
            dockerClient.removeContainerCmd(containerId).exec();

            dockerContainerRepository.deleteById(containerId);

            return ResponseEntity.ok("컨테이너 삭제 완료");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제할 컨테이너 찾을 수 없음");
    }
}
