// TtydLauncherService.java
package org.mjuecs.mjuecs.service;

import lombok.RequiredArgsConstructor;
import org.mjuecs.mjuecs.DockerClientFactory;
import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.domain.TtydContainer;
import org.mjuecs.mjuecs.repository.DockerContainerRepository;
import org.mjuecs.mjuecs.repository.TtydContainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class TtydService {

    private final TtydContainerRepository ttydContainerRepository;
    private final DockerContainerRepository dockerContainerRepository;
    private final PortAllocator portAllocator;



    public void launchTtydAndSave(DockerContainer dockerContainer) {
        int ttydPort = portAllocator.allocatePort();
        String containerId = dockerContainer.getContainerId();

        String ttydContainerId = runTtydProxy(containerId, ttydPort);

        TtydContainer ttyd = new TtydContainer();
        ttyd.setTtydContainerId(ttydContainerId);
        ttyd.setDockerContainer(dockerContainer);
        ttydContainerRepository.save(ttyd);

        dockerContainer.setTtydPort(ttydPort);
        dockerContainerRepository.save(dockerContainer);
    }

    private String runTtydProxy(String containerId, int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./deploy-ttyd.sh", containerId, String.valueOf(port));
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            pb.start();

            // Docker CLI로 실제 ttyd 컨테이너 ID를 확인
            Thread.sleep(1000); // 약간의 지연 필요
            return waitForContainerId(port);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("ttyd 실행 실패", e);
        }
    }

    private String waitForContainerId(int port) {
        return DockerClientFactory.createClient().listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(c -> Arrays.asList(c.getNames()).contains("/ttyd-proxy-" + port))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ttyd 컨테이너 생성 실패"))
                .getId();
    }

    public void stopTtyd(String dockerContainerId) {
        ttydContainerRepository.findByDockerContainer_ContainerId(dockerContainerId).ifPresent(ttyd -> {
            try {
                new ProcessBuilder("docker", "stop", ttyd.getTtydContainerId()).start();
            } catch (IOException e) {
                throw new RuntimeException("ttyd 중지 실패", e);
            }
        });
    }

    public void startTtyd(String dockerContainerId) {
        ttydContainerRepository.findByDockerContainer_ContainerId(dockerContainerId).ifPresent(ttyd -> {
            try {
                new ProcessBuilder("docker", "start", ttyd.getTtydContainerId()).start();
            } catch (IOException e) {
                throw new RuntimeException("ttyd 시작 실패", e);
            }
        });
    }

    public void restartTtyd(String dockerContainerId) {
        ttydContainerRepository.findByDockerContainer_ContainerId(dockerContainerId).ifPresent(ttyd -> {
            try {
                new ProcessBuilder("docker", "restart", ttyd.getTtydContainerId()).start();
            } catch (IOException e) {
                throw new RuntimeException("ttyd 재시작 실패", e);
            }
        });
    }

    public void removeTtyd(String dockerContainerId) {
        ttydContainerRepository.findByDockerContainer_ContainerId(dockerContainerId).ifPresent(ttyd -> {
            try {
                new ProcessBuilder("docker", "rm", "-f", ttyd.getTtydContainerId()).start();
                ttydContainerRepository.delete(ttyd);
            } catch (IOException e) {
                throw new RuntimeException("ttyd 삭제 실패", e);
            }
        });
    }


//    /**
//     * ttyd를 실행하여 컨테이너에 대한 ttyd 세션을 시작합니다.
//     * @param containerId
//     * @return ttyd 접근 URL (예: http://localhost:포트번호) 실패시 null 반환
//     */
//    public String launchTtydForContainer(String containerId) {
//        int hostPort = portAllocator.allocatePort();
//        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./deploy-ttyd.sh", containerId, String.valueOf(hostPort));
//        pb.directory(new File(".")); // 스크립트 위치 기준 경로
//        pb.redirectErrorStream(true);
//
//        try {
//            Process process = pb.start();
//            System.out.println("[ttyd] 실행됨: 컨테이너 " + containerId + ", 포트 " + hostPort);
//            return "http://localhost:" + hostPort;
//        } catch (IOException e) {
//            System.err.println("[ttyd] 실행 실패: " + e.getMessage());
//            return null;
//        }
//    }
}
