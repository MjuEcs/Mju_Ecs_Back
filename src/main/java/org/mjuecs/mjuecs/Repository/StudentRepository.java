package org.mjuecs.mjuecs.Repository;

import org.mjuecs.mjuecs.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student,String> {
}
