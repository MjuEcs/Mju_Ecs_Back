package org.mjuecs.mjuecs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerStatusDto {
    private String containerId;
    private String status;
    private String image;
    private String startedAt;
    private ContainerPortsDto ports;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerPortsDto {
        private int hostPort;
        private int containerPort;
        private int ttydHostPort;
    }
}
