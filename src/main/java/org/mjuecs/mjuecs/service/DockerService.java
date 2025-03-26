package org.mjuecs.mjuecs.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.springframework.stereotype.Service;

@Service
public class DockerService {

    private final DockerClient dockerClient;

    public DockerService() {
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
    public String createAndStartContainer(String imageName) {
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

        return container.getId();
    }

    // 3. 컨테이너 삭제 (중지 → 삭제)
    public void removeContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();  // 실패해도 넘어감
        } catch (NotModifiedException e) {
            // 이미 중지된 상태, 무시
        }
        dockerClient.removeContainerCmd(containerId).exec();
    }
}
