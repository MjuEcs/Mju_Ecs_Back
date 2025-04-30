package org.mjuecs.mjuecs.repository;

import org.mjuecs.mjuecs.domain.DockerContainer;
import org.mjuecs.mjuecs.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DockerContainerRepository extends JpaRepository<DockerContainer,String> {
    List<DockerContainer> findByStudent(Student student);
}
