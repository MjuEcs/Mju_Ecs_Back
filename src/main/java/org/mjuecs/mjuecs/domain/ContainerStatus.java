package org.mjuecs.mjuecs.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerStatus {
    private String containerId;
    private String status;
    private String image;
    private Instant startedAt;
    private int hostPort;
    private int containerPort;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerStatus that = (ContainerStatus) o;
        return status.equals(that.status) && startedAt.equals(that.startedAt);
    }

    @Override
    public int hashCode() {
        return containerId.hashCode();
    }
}

