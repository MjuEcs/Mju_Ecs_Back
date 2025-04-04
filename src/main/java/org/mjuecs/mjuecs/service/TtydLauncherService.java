// TtydLauncherService.java
package org.mjuecs.mjuecs.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TtydLauncherService {

    public boolean launchTtydForContainer(String containerId, int hostPort) {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./deploy-ttyd.sh", containerId, String.valueOf(hostPort));
        pb.directory(new File(".")); // 스크립트 위치 기준 경로
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            System.out.println("[ttyd] 실행됨: 컨테이너 " + containerId + ", 포트 " + hostPort);
            return true;
        } catch (IOException e) {
            System.err.println("[ttyd] 실행 실패: " + e.getMessage());
            return false;
        }
    }
}
