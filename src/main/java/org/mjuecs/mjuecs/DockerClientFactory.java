package org.mjuecs.mjuecs;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerClientFactory {

    public static DockerClient createClient() {
        // 1. 기본 config 설정 (Mac에서는 unix socket 사용)
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")  // Mac/Linux 로컬용
                .build();

        // 2. OkHttp 기반 Docker HTTP Client 생성
        DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        // 3. DockerClient 인스턴스 생성
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
