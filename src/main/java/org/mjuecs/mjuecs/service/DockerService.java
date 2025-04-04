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

    // 2. 컨테이너 생성 및 시작
    public ResponseEntity<?> createContainer(String imageName, Student student) {
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

    public String createCustomContainer(ContainerDto containerDto, Student student) {
        if (dockerContainerRepository.findByStudent(student).size() < 2) {
            String image = containerDto.getImageName();
            Map<String, String> envVars = containerDto.getEnv();
            int hostPort = containerDto.getHostPort();

            pullImageIfNotExists(image);

            List<String> envList = envVars.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();

            HostConfig hostConfig;
            if (hostPort > 0) {
                // 명시적 포트 요청 시에만 포트 바인딩 수행
                ExposedPort containerPort = ExposedPort.tcp(hostPort);
                Ports.Binding binding = Ports.Binding.bindPort(hostPort);
                hostConfig = HostConfig.newHostConfig()
                        .withPortBindings(new PortBinding(binding, containerPort));
            } else {
                hostConfig = HostConfig.newHostConfig();
            }

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withName("MjuEcs-" + student.getStudentId() + "-" + (dockerContainerRepository.findByStudent(student).size() + 1))
                    .withEnv(envList)
                    .withHostConfig(hostConfig)
                    .withCmd(containerDto.getCmd())
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withStdinOpen(true)
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            DockerContainer dockerContainer = new DockerContainer();
            dockerContainer.setContainerId(container.getId());
            dockerContainer.setImage(image);
            dockerContainer.setStudent(student);
            dockerContainerRepository.save(dockerContainer);

            return container.getId();
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
