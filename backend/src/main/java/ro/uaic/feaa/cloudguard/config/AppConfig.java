package ro.uaic.feaa.cloudguard.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ro.uaic.feaa.cloudguard.service.audit.ClientIpProvider;

/**
 * Configurația aplicației Spring: unde sunt entitățile și repository-urile,
 * plus bean-urile de suport folosite de serviciile de business.
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "ro.uaic.feaa.cloudguard.domain.model")
@EnableJpaRepositories(basePackages = "ro.uaic.feaa.cloudguard.domain.repository")
public class AppConfig {

    /** Ceasul aplicației; în teste este înlocuit cu un ceas fix. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Adresa IP salvată în jurnalul de audit. Până la stratul REST
     * folosim adresa locală; controller-ele vor furniza IP-ul real al cererii.
     */
    @Bean
    public ClientIpProvider clientIpProvider() {
        return () -> "127.0.0.1";
    }
}
