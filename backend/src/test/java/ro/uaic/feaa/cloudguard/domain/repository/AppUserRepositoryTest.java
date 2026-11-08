package ro.uaic.feaa.cloudguard.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ro.uaic.feaa.cloudguard.domain.model.AppUser;
import ro.uaic.feaa.cloudguard.domain.model.Team;
import ro.uaic.feaa.cloudguard.domain.model.UserRole;

/** Teste pentru repository-urile entităților de suport: utilizatori și echipe. */
@DataJpaTest
class AppUserRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private AppUserRepository userRepository;
    @Autowired private TeamRepository teamRepository;

    @BeforeEach
    void setUp() {
        Team platform = em.persist(new Team("Platform"));
        em.persist(new Team("Marketing"));
        em.persist(new AppUser("emanuel", "emanuel@firma.ro", UserRole.RESOURCE_OWNER, platform));
        em.persist(new AppUser("delia", "delia@firma.ro", UserRole.SECURITY_OFFICER, null));
        em.flush();
        em.clear();
    }

    @Test
    void usersAreFoundByUsernameEmailAndRole() {
        assertThat(userRepository.findByUsername("delia")).isPresent();
        assertThat(userRepository.findByEmail("emanuel@firma.ro")).isPresent();
        assertThat(userRepository.findByRole(UserRole.SECURITY_OFFICER)).extracting(AppUser::getUsername)
                .containsExactly("delia");
        assertThat(userRepository.findByUsername("nimeni")).isEmpty();
    }

    @Test
    void teamsAreFoundByNameFragment() {
        assertThat(teamRepository.findByNameContainingIgnoreCase("plat")).extracting(Team::getName)
                .containsExactly("Platform");
        assertThat(teamRepository.findByName("Marketing")).isPresent();
    }

    @Test
    void userKeepsItsTeamAssociation() {
        AppUser emanuel = userRepository.findByUsername("emanuel").orElseThrow();

        assertThat(emanuel.getTeam().getName()).isEqualTo("Platform");
        assertThat(emanuel.getRole()).isEqualTo(UserRole.RESOURCE_OWNER);
    }
}
