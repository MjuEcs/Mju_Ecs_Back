package org.mjuecs.mjuecs.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class Student {
    @Id
    String studentId;

    String name;

    Date lastLogin;
}
