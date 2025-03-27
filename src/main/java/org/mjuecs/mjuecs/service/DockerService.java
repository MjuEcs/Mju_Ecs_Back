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
import org.springframework.stereotype.Service;

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
            dockerClient.pullImageCmd(imageName)
                    .start();
//                    .forEachRemaining(PullResponseItem::toString);
        }
    }

    // 2. 컨테이너 생성 및 시작
    public String createAndStartContainer(String imageName, Student student) {
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

            return container.getId();
        }
        return "컨테이너 생성 불가";
    }

    // 3. 컨테이너 삭제 (중지 → 삭제)
    public void removeContainer(String containerId) {
        //데이터 베이스 확인하고 컨테이너 아이디 있으면 삭제하고
        try {
            dockerClient.stopContainerCmd(containerId).exec();  // 실패해도 넘어감
        } catch (NotModifiedException e) {
            // 이미 중지된 상태, 무시
        }
        dockerClient.removeContainerCmd(containerId).exec();
    }
}
