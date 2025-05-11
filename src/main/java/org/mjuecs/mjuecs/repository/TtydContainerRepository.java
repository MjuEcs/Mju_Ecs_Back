package org.mjuecs.mjuecs.repository;

import org.mjuecs.mjuecs.domain.TtydContainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TtydContainerRepository extends JpaRepository<TtydContainer,String> {
    Optional<TtydContainer> findByDockerContainer_ContainerId(String containerId);
}
