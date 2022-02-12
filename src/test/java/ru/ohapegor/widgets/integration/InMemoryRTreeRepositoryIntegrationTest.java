package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "widgets.repository=in-memory-r-tree")
@ActiveProfiles({"mem", "r-tree"})
class InMemoryRTreeRepositoryIntegrationTest extends AbstractWidgetsIntegrationTest {
}
