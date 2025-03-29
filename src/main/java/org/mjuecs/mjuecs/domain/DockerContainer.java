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
}
