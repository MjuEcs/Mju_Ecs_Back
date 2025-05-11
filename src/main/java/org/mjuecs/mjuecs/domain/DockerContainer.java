package org.mjuecs.mjuecs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DockerContainer {
    @Id
    private String containerId;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String image;//컨테이너 생성에 사용된 이미지
    //이미지를 미리 만들거나 저장해둔다면 따로 도메인 생성 필요

    private int containerPort;                   //컨테이너 포트
    private int hostPort;                        //호스트 포트
    
    // private String ttydContainerId;           //ttyd 컨테이너 ID
    // private String ttydHostPort;              //ttyd 호스트 포트
    // ttyd 컨테이너의 컨테이너 포트의 경우 고정되어 있으므로 필요 없음

    // private String volumeId;                  //볼륨 ID
}
