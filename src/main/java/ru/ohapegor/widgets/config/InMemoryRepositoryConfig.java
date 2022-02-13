package ru.ohapegor.widgets.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.repository.memory.InMemoryMapsWidgetsRepository;
import ru.ohapegor.widgets.repository.memory.InMemoryRTreeWidgetsRepository;

@Configuration
@Slf4j
public class InMemoryRepositoryConfig {

    @Bean
    @ConditionalOnProperty(value = "widgets.repository", havingValue = "maps")
    public WidgetsRepository inMemoryMapWidgetsRepository() {
        log.info("initializing context with widgets in memory repository implementation of 2 maps");
        return new InMemoryMapsWidgetsRepository();
    }

    @Bean
    @ConditionalOnProperty(value = "widgets.repository", havingValue = "r-tree")
    public WidgetsRepository inMemoryRtreeWidgetsRepository() {
        log.info("initializing context with widgets in memory r-tree repository implementation");
        return new InMemoryRTreeWidgetsRepository();
    }
}
