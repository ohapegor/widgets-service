package ru.ohapegor.widgets.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.repository.memory.InMemoryMapsWidgetsRepository;
import ru.ohapegor.widgets.repository.memory.InMemoryRTreeWidgetsRepository;

@Configuration
public class InMemoryRepositoryConfig {

    @Bean
    @ConditionalOnProperty(value = "widgets.repository", havingValue = "maps")
    public WidgetsRepository inMemoryMapWidgetsRepository() {
        return new InMemoryMapsWidgetsRepository();
    }

    @Bean
    @ConditionalOnProperty(value = "widgets.repository", havingValue = "r-tree")
    public WidgetsRepository inMemoryRtreeWidgetsRepository() {
        return new InMemoryRTreeWidgetsRepository();
    }
}
