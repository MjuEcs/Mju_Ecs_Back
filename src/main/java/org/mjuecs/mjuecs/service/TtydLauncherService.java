// TtydLauncherService.java
package org.mjuecs.mjuecs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TtydLauncherService {

    @Autowired
    private PortAllocator portAllocator;


    /**
     * ttyd를 실행하여 컨테이너에 대한 ttyd 세션을 시작합니다.
     * @param containerId
     * @return ttyd 접근 URL (예: http://localhost:포트번호) 실패시 null 반환
     */
    public String launchTtydForContainer(String containerId) {
        int hostPort = portAllocator.allocatePort();
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./deploy-ttyd.sh", containerId, String.valueOf(hostPort));
        pb.directory(new File(".")); // 스크립트 위치 기준 경로
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            System.out.println("[ttyd] 실행됨: 컨테이너 " + containerId + ", 포트 " + hostPort);
            return "http://localhost:" + hostPort;
        } catch (IOException e) {
            System.err.println("[ttyd] 실행 실패: " + e.getMessage());
            return null;
        }
    }
}
