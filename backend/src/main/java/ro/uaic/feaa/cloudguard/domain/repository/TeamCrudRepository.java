package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.Optional;

import ro.uaic.feaa.cloudguard.domain.model.Team;

/** (Bonus) Repository CRUD pentru Team; implementarea cu EntityManager este TeamRepositoryJPA. */
public interface TeamCrudRepository extends IRepository<Team, Long> {

    Optional<Team> findByName(String name);
}
