package ru.ohapegor.widgets.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.repository.database.WidgetsDataJpaRepository;
import ru.ohapegor.widgets.repository.database.WidgetsH2Repository;

import javax.persistence.EntityManager;

@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(value = "widgets.repository", havingValue = "h2")
@Slf4j
public class JPARepositoryConfig {

    @Bean
    public WidgetsRepository repository(WidgetsDataJpaRepository dataJpaRepository, EntityManager entityManager) {
        log.info("initializing context with widgets in memory h2 repository implementation");
        return new WidgetsH2Repository(dataJpaRepository, entityManager);
    }

}
