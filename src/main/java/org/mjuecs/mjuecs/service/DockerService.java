package org.mjuecs.mjuecs.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.mjuecs.mjuecs.Repository.DockerContainerRepository;
import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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


    private ExposedPort guessDefaultPortByImage(String imageName) {
        imageName = imageName.toLowerCase();

        if (imageName.contains("mysql")) return ExposedPort.tcp(3306);
        if (imageName.contains("postgres")) return ExposedPort.tcp(5432);
        if (imageName.contains("nginx")) return ExposedPort.tcp(80);
        if (imageName.contains("redis")) return ExposedPort.tcp(6379);
        if (imageName.contains("mongo")) return ExposedPort.tcp(27017);
        if (imageName.contains("ubuntu") || imageName.contains("kali")) return ExposedPort.tcp(22); // SSH 등

        // 기본 포트 없으면 fallback (임시 포트 사용 or 예외 처리)
        return ExposedPort.tcp(8080);
    }

    // 2. 컨테이너 생성 및 시작
    public ResponseEntity<?> createAndStartContainer(String imageName, Student student) {
        //학생당 컨테이너가 2개 이상 생성불가
        if(dockerContainerRepository.findByStudent(student).size()<2) {
            pullImageIfNotExists(imageName);

            ExposedPort containerPort = ExposedPort.tcp(80);
            Ports.Binding hostPortBinding = Ports.Binding.bindPort(8081);  // localhost:8081

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(new PortBinding(hostPortBinding, containerPort));

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName("MjuEcs-container-" + System.currentTimeMillis()) // 이름 겹치지 않게
                    .withExposedPorts(containerPort)
                    .withHostConfig(hostConfig)
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            //컨테이너 정보 저장
            DockerContainer dockerContainer = new DockerContainer();
            dockerContainer.setContainerId(container.getId());
            dockerContainer.setImage(imageName);
            dockerContainer.setStudent(student);
            dockerContainerRepository.save(dockerContainer);

            return ResponseEntity.ok(container.getId());
        }
        return ResponseEntity.ok("컨테이너 생성 불가");
    }

    public ResponseEntity<?> createCustomContainer(ContainerDto containerDto,Student student) {

        if(dockerContainerRepository.findByStudent(student).size()<2) {
            String image = containerDto.getImageName();
            Map<String, String> envVars = containerDto.getEnv();
            int hostPort = containerDto.getHostPort();

            // 1. 이미지 없으면 pull
            pullImageIfNotExists(image);

            // 2. 포트 바인딩 설정 (예: 3306 or 22 or 8080 depending on image)
            ExposedPort containerPort = guessDefaultPortByImage(image);
            Ports.Binding binding = Ports.Binding.bindPort(hostPort);
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(new PortBinding(binding, containerPort));

            // 3. env 리스트 변환
            List<String> envList = envVars.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();

            // 4. 컨테이너 생성
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withExposedPorts(containerPort)
                    .withEnv(envList)
                    .withHostConfig(hostConfig)
                    .withCmd(containerDto.getCmd())  // ubuntu 등에는 "sleep" 같은 cmd 필요
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            DockerContainer dockerContainer = new DockerContainer();
            dockerContainer.setContainerId(container.getId());
            dockerContainer.setImage(image);
            dockerContainer.setStudent(student);
            dockerContainerRepository.save(dockerContainer);

            return ResponseEntity.ok(container.getId());
        }
        return ResponseEntity.ok("컨테이너 생성 불가");
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
        return ResponseEntity.status(404).body("삭제할 컨테이너 찾을 수 없음");
    }
}
