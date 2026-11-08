package ro.uaic.feaa.cloudguard.domain.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.repository.TeamCrudRepository;

/** (Bonus) Repository complet JPA: aceleași operații ca JpaRepository, scrise cu EntityManager. */
@Repository
@Transactional
public class TeamRepositoryJPA implements TeamCrudRepository {

    @Autowired
    private EntityManager entityManager;

    @Override
    public Team save(Team team) {
        if (team.getId() == null) {
            entityManager.persist(team);
            return team;
        }
        return entityManager.merge(team);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Team.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAll() {
        return entityManager.createQuery("select t from Team t order by t.name", Team.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findByName(String name) {
        return entityManager.createQuery("select t from Team t where t.name = :name", Team.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void delete(Team team) {
        entityManager.remove(entityManager.contains(team) ? team : entityManager.merge(team));
    }

    @Override
    public void deleteById(Long id) {
        findById(id).ifPresent(this::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return entityManager.createQuery("select count(t) from Team t", Long.class).getSingleResult();
    }
}
