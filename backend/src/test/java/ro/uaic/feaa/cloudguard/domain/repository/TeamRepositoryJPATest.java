package ro.uaic.feaa.cloudguard.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.repository.jpa.TeamRepositoryJPA;

/** (Bonus) Testează repository-ul complet JPA, scris cu EntityManager, pe interfața CRUD proprie. */
@DataJpaTest
@Import(TeamRepositoryJPA.class)
class TeamRepositoryJPATest {

    @Autowired private TeamCrudRepository repository;

    @Test
    void savePersistsANewTeamAndAssignsAnId() {
        Team saved = repository.save(new Team("Platform"));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void findByNameAndFindAllWork() {
        repository.save(new Team("Platform"));
        repository.save(new Team("Marketing"));

        assertThat(repository.findByName("Marketing")).isPresent();
        assertThat(repository.findByName("Nu există")).isEmpty();
        assertThat(repository.findAll()).extracting(Team::getName).containsExactly("Marketing", "Platform");
    }

    @Test
    void deleteAndDeleteByIdRemoveTheTeam() {
        Team platform = repository.save(new Team("Platform"));
        Team marketing = repository.save(new Team("Marketing"));

        repository.delete(platform);
        repository.deleteById(marketing.getId());

        assertThat(repository.count()).isZero();
    }

    @Test
    void deleteByIdOfAnUnknownTeamDoesNothing() {
        repository.save(new Team("Platform"));

        repository.deleteById(9999L);

        assertThat(repository.count()).isEqualTo(1);
    }
}
