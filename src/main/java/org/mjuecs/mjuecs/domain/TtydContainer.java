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

    private String ttydPasswd;
    //ttyd 컨피덴셜 맵핑해서 유져이름은 그냥 학번, 비밀번호 랜덤(생성해서) + 비밀번호 조회 엔드포인트 생성(컨트롤러)

}
