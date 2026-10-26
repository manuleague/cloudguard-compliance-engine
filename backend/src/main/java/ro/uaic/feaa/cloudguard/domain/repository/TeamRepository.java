package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.uaic.feaa.cloudguard.domain.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByName(String name);
}
