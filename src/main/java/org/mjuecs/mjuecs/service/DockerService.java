package org.mjuecs.mjuecs.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mjuecs.mjuecs.DockerClientFactory;
import org.mjuecs.mjuecs.Repository.DockerContainerRepository;
import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.mjuecs.mjuecs.exception.ContainerLimitExceededException;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final DockerContainerRepository dockerContainerRepository;

    public DockerService(DockerContainerRepository dockerContainerRepository) {
        this.dockerContainerRepository = dockerContainerRepository;
        this.dockerClient = DockerClientFactory.createClient();
    }

    private ExposedPort guessDefaultPortByImage(String imageName) {
        imageName = imageName.toLowerCase();

        if (imageName.contains("mysql")) return ExposedPort.tcp(3306);
        if (imageName.contains("postgres")) return ExposedPort.tcp(5432);
        if (imageName.contains("nginx")) return ExposedPort.tcp(80);
        if (imageName.contains("redis")) return ExposedPort.tcp(6379);
        if (imageName.contains("oracle-xe")) return ExposedPort.tcp(1521);
        if (imageName.contains("mongo")) return ExposedPort.tcp(27017);
        if (imageName.contains("ubuntu") || imageName.contains("kali")) return ExposedPort.tcp(22); // SSH 등

        // 기본 포트 없으면 fallback (임시 포트 사용 or 예외 처리)
        return ExposedPort.tcp(8080);
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
    public String createAndStartContainer(String imageName, Student student) {
        // 학생당 컨테이너가 2개 이상 생성불가
        if (dockerContainerRepository.findByStudent(student).size() >= 2) {
            return "이미지 생성 실패: 학생당 2개 이상 생성 불가";
        }

        pullImageIfNotExists(imageName);

        ExposedPort containerPort = ExposedPort.tcp(80);
        Ports.Binding hostPortBinding = Ports.Binding.bindPort(8081); // localhost:8081

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(new PortBinding(hostPortBinding, containerPort));

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName("MjuEcs-container-" + System.currentTimeMillis()) // 이름 겹치지 않게
                .withExposedPorts(containerPort)
                .withHostConfig(hostConfig)
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        // 컨테이너 정보 저장
        DockerContainer dockerContainer = new DockerContainer();
        dockerContainer.setContainerId(container.getId());
        dockerContainer.setImage(imageName);
        dockerContainer.setStudent(student);
        dockerContainerRepository.save(dockerContainer);

        return container.getId();
    }

    public String createCustomContainer(ContainerDto containerDto, Student student) {
        if (dockerContainerRepository.findByStudent(student).size() >= 2) {
            throw new ContainerLimitExceededException("학생당 최대 2개의 컨테이너만 생성 가능합니다");
        }

        String image = containerDto.getImageName();
        Map<String, String> envVars = containerDto.getEnv();
        int hostPort = containerDto.getHostPort();

        // 1. 이미지 없으면 pull
        pullImageIfNotExists(image);

        // 2. 포트 바인딩 설정
        ExposedPort containerPort = guessDefaultPortByImage(image);
        Ports.Binding binding = Ports.Binding.bindPort(hostPort);
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(new PortBinding(binding, containerPort))
                .withCpuCount(2L) // CPU 코어 수 제한 (2코어)
                .withMemory(4L * 1024 * 1024 * 1024) // 메모리 제한 (4GB)
                .withMemorySwap(4L * 1024 * 1024 * 1024); // Swap 메모리도 동일하게 제한

        // 3. env 리스트 변환
        List<String> envList = envVars.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();

        // 4. 컨테이너 생성
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withExposedPorts(containerPort)
                .withEnv(envList)
                .withHostConfig(hostConfig)
                .withCmd(containerDto.getCmd())
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        DockerContainer dockerContainer = new DockerContainer();
        dockerContainer.setContainerId(container.getId());
        dockerContainer.setImage(image);
        dockerContainer.setStudent(student);
        dockerContainerRepository.save(dockerContainer);

        return container.getId();
    }

    // 3. 컨테이너 삭제 (중지 → 삭제)
    public void removeContainer(String containerId) {
        Optional<DockerContainer> dockerContainer = dockerContainerRepository.findById(containerId);
        // 데이터 베이스 확인하고 컨테이너 아이디 있으면 삭제하고
        if (dockerContainer.isPresent()) {
            // 데이터 베이스 확인하고 컨테이너 아이디 있으면 삭제하고
            try {
                dockerClient.stopContainerCmd(containerId).exec(); // 실패해도 넘어감
            } catch (NotModifiedException e) {
                // 이미 중지된 상태, 무시
            }
            dockerClient.removeContainerCmd(containerId).exec();

            dockerContainerRepository.deleteById(containerId);
        }
    }
}
