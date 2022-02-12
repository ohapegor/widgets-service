package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "widgets.repository=in-memory-map")
@ActiveProfiles({"mem", "map"})
class InMemoryMapRepositoryIntegrationTest extends AbstractWidgetsIntegrationTest{
}
