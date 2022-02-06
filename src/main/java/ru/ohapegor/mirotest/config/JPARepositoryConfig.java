package ru.ohapegor.mirotest.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import ru.ohapegor.mirotest.repository.WidgetsRepository;
import ru.ohapegor.mirotest.repository.database.WidgetsDataJpaRepository;
import ru.ohapegor.mirotest.repository.database.WidgetsH2Repository;

import javax.persistence.EntityManager;

@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(value = "widgets.repository", havingValue = "h2")
public class JPARepositoryConfig {

    @Bean
    public WidgetsRepository repository(WidgetsDataJpaRepository dataJpaRepository, EntityManager entityManager) {
        return new WidgetsH2Repository(dataJpaRepository, entityManager);
    }

}
