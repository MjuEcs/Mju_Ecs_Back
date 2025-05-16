package org.mjuecs.mjuecs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ContainerStatusDto {
    private String containerId;
    private String status;
    private String image;
    private Instant startedAt;
    private int hostPort;
    private int containerPort;
}