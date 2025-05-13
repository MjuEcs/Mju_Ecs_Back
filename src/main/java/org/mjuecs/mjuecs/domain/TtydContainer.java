package org.mjuecs.mjuecs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TtydContainer {
    @Id
    private String ttydContainerId;

    @OneToOne
    @JoinColumn(name = "docker_container_id")
    private DockerContainer dockerContainer;

}
