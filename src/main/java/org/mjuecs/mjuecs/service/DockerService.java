package org.mjuecs.mjuecs.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.mjuecs.mjuecs.repository.DockerContainerRepository;
import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.domain.Student;
import org.mjuecs.mjuecs.dto.ContainerDto;
import org.mjuecs.mjuecs.dto.ContainerStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final DockerContainerRepository dockerContainerRepository;
    private final PortAllocator portAllocator;
    private final TtydService ttydService;

    public DockerService(DockerContainerRepository dockerContainerRepository, PortAllocator portAllocator, TtydService ttydService) {
        this.dockerContainerRepository = dockerContainerRepository;
        this.ttydService = ttydService;
        this.dockerClient = DockerClientFactory.createClient();
        this.portAllocator = portAllocator;
    }

    // 이미지 존재 여부 체크 후 없으면 pull
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

    // 커스텀 컨테이너 생성(실행까지)
    public String createCustomContainer(ContainerDto dto, Student student) {
        if (dockerContainerRepository.findByStudent(student).size() < 2) {
            pullImageIfNotExists(dto.getImageName());

            // HostConfig 설정: 메모리, CPU, 포트 바인딩
            int hostPort = portAllocator.allocatePort();
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(3L * 1024 * 1024 * 1024) // 3GB
                    .withCpuCount(2L) // 2코어
                    .withPortBindings(
                            new PortBinding(
                                    Ports.Binding.bindPort(hostPort), // 호스트 포트
                                    new ExposedPort(dto.getContainerPort(), InternetProtocol.TCP) // 컨테이너 내부 포트
                            ));
            
            ExposedPort exposedPort = new ExposedPort(dto.getContainerPort(), InternetProtocol.TCP);

            CreateContainerCmd baseCmd = dockerClient.createContainerCmd(dto.getImageName());

            if (dto.getHostName() != null && !dto.getHostName().isBlank()) {
                baseCmd.withHostName(dto.getHostName());
            }

            CreateContainerResponse container = baseCmd
                    .withName("MjuEcs-" + student.getStudentId() + "-" + System.currentTimeMillis())
                    .withEnv(dto.getEnv().entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue()).toList())
                    .withHostConfig(hostConfig)
                    .withExposedPorts(exposedPort)
                    .withCmd(dto.getCmd())
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withStdinOpen(true)
                    .exec();

            //hostName
            // 메모리 3기가에,2코어
            // 컨테이너 안에 볼륨 맵핑 db
            // status 요청 5s

            dockerClient.startContainerCmd(container.getId()).exec();

            DockerContainer dockerContainer = new DockerContainer();
            dockerContainer.setContainerId(container.getId());
            dockerContainer.setImage(dto.getImageName());
            dockerContainer.setStudent(student);
            dockerContainer.setHostPort(hostPort); // 외부 포트 저장
            dockerContainer.setContainerPort(dto.getContainerPort()); // 내부 포트 저장
            dockerContainerRepository.save(dockerContainer);

            ttydService.launchTtydAndSave(dockerContainer);

            return container.getId(); // ✅ 사용자에게 URL 제공
        }
        return "컨테이너 생성 불가";
    }

    // 존재 검증, 권한 검증 함수
    private ResponseEntity<?> validateContainerOwnership(String containerId, Student student) {
        Optional<DockerContainer> dockerContainerOpt = dockerContainerRepository.findById(containerId);

        if (dockerContainerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 컨테이너");
        }

        DockerContainer dockerContainer = dockerContainerOpt.get();
        if (!student.equals(dockerContainer.getStudent())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한 없음");
        }

        return null; // 유효할 경우 null 반환
    }

    // 컨테이너 시작
    public ResponseEntity<?> startContainer(String containerId, Student student) {
        ResponseEntity<?> validationResponse = validateContainerOwnership(containerId, student);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            dockerClient.startContainerCmd(containerId).exec();
            ttydService.startTtyd(containerId);
            return ResponseEntity.ok("컨테이너 시작 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("시작 실패: " + e.getMessage());
        }
    }

    public ResponseEntity<?> stopContainer(String containerId, Student student) {
        ResponseEntity<?> validationResponse = validateContainerOwnership(containerId, student);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            dockerClient.stopContainerCmd(containerId).exec();
            ttydService.stopTtyd(containerId);
            return ResponseEntity.ok("컨테이너 중지 완료");
        } catch (NotModifiedException e) {
            return ResponseEntity.ok("이미 중지된 상태");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("중지 실패: " + e.getMessage());
        }
    }

    // 컨테이너 재시작
    public ResponseEntity<?> restartContainer(String containerId, Student student) {
        ResponseEntity<?> validationResponse = validateContainerOwnership(containerId, student);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            dockerClient.restartContainerCmd(containerId).exec();
            ttydService.restartTtyd(containerId);
            return ResponseEntity.ok("컨테이너 재시작 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("재시작 실패: " + e.getMessage());
        }
    }

    // 컨테이너 삭제
    public ResponseEntity<?> removeContainer(String containerId, Student student) {
        ResponseEntity<?> validationResponse = validateContainerOwnership(containerId, student);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            // 이미 실행 중이라면 중지
            try {
                dockerClient.stopContainerCmd(containerId).exec();
                ttydService.removeTtyd(containerId);// NotModifiedException 무시
            } catch (NotModifiedException ignored) {
            }

            dockerClient.removeContainerCmd(containerId).exec();
            dockerContainerRepository.deleteById(containerId);
            return ResponseEntity.ok("컨테이너 삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 실패: " + e.getMessage());
        }
    }

    // 컨테이너 상태 조회 (container Id 기반 조회)
    public ResponseEntity<?> getContainerStatus(String containerId, Student student) {
        ResponseEntity<?> validationResponse = validateContainerOwnership(containerId, student);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            DockerContainer dockerContainer = dockerContainerRepository.findById(containerId).orElseThrow();

            ContainerStatusDto.ContainerPortsDto portsDto = ContainerStatusDto.ContainerPortsDto.builder()
                    .hostPort(dockerContainer.getHostPort())
                    .containerPort(dockerContainer.getContainerPort())
                    .ttydHostPort(dockerContainer.getTtydHostPort())
                    .build();

            ContainerStatusDto statusDto = ContainerStatusDto.builder()
                    .containerId(dockerContainer.getContainerId())
                    .status(containerInfo.getState().getStatus())
                    .image(dockerContainer.getImage())
                    .startedAt(containerInfo.getState().getStartedAt())
                    .ports(portsDto)
                    .build();

            return ResponseEntity.ok(List.of(statusDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("컨테이너 상태 조회 실패: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getAllContainerStatuses(Student student) {
        List<DockerContainer> containers = dockerContainerRepository.findByStudent(student);
        List<ContainerStatusDto> result = new ArrayList<>();

        for (DockerContainer container : containers) {
            try {
                InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(container.getContainerId())
                        .exec();

                ContainerStatusDto.ContainerPortsDto portsDto = ContainerStatusDto.ContainerPortsDto.builder()
                        .hostPort(container.getHostPort())
                        .containerPort(container.getContainerPort())
                        .ttydHostPort(container.getTtydHostPort())
                        .build();

                ContainerStatusDto statusDto = ContainerStatusDto.builder()
                        .containerId(container.getContainerId())
                        .status(containerInfo.getState().getStatus())
                        .image(container.getImage())
                        .startedAt(containerInfo.getState().getStartedAt())
                        .ports(portsDto)
                        .build();

                result.add(statusDto);
            } catch (Exception e) {
                ContainerStatusDto.ContainerPortsDto portsDto = ContainerStatusDto.ContainerPortsDto.builder()
                        .hostPort(container.getHostPort())
                        .containerPort(container.getContainerPort())
                        .ttydHostPort(container.getTtydHostPort())
                        .build();

                ContainerStatusDto statusDto = ContainerStatusDto.builder()
                        .containerId(container.getContainerId())
                        .status("조회 실패: " + e.getMessage())
                        .image(container.getImage())
                        .startedAt("")
                        .ports(portsDto)
                        .build();
                        
                result.add(statusDto);
            }
        }

        return ResponseEntity.ok(result);
    }

    public File downloadContainerFiles(String containerId) throws IOException {
        // Temporary directory to store copied files
        Path tempDir = Files.createTempDirectory("docker-container-files");

        // Execute docker cp command using ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder("docker", "cp", containerId + ":/", tempDir.toString());
        Process process = processBuilder.start();

        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroy();
                throw new IOException("Failed to copy files from container: " + containerId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted while copying files", e);
        }

        // Create a zip file from the copied files
        Path zipFile = Files.createTempFile( containerId + "-volume", ".zip"); // 
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walk(tempDir).forEach(path -> {
                try {
                    if (Files.isRegularFile(path)) {
                        ZipEntry zipEntry = new ZipEntry(tempDir.relativize(path).toString());
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        // Clean up temporary directory
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

        return zipFile.toFile();
    }
}
