package ru.ohapegor.widgets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.repository.memory.InMemoryMapsWidgetsRepository;
import ru.ohapegor.widgets.repository.memory.InMemoryRTreeWidgetsRepository;

@Configuration
@Profile("mem")
public class InMemoryRepositoryConfig {

    @Bean
    @Profile("map")
    //@ConditionalOnProperty(value = "widgets.repository", havingValue = "in-memory-map")
    public WidgetsRepository inMemoryMapWidgetsRepository() {
        return new InMemoryMapsWidgetsRepository();
    }

    @Bean
    @Profile("r-tree")
    //@ConditionalOnProperty(value = "widgets.repository", havingValue = "in-memory-r-tree")
    public WidgetsRepository inMemoryRtreeWidgetsRepository() {
        return new InMemoryRTreeWidgetsRepository();
    }
}
