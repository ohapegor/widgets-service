package ru.ohapegor.widgets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.repository.database.WidgetsDataJpaRepository;
import ru.ohapegor.widgets.repository.database.WidgetsH2Repository;

import javax.persistence.EntityManager;

@Configuration
@EnableJpaAuditing
@Profile("h2")
//@ConditionalOnProperty(value = "widgets.repository", havingValue = "h2")
public class JPARepositoryConfig {

    @Bean
    public WidgetsRepository repository(WidgetsDataJpaRepository dataJpaRepository, EntityManager entityManager) {
        return new WidgetsH2Repository(dataJpaRepository, entityManager);
    }

}
