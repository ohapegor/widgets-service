package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "widgets.repository=h2")
@ActiveProfiles("h2")
class H2RepositoryIntegrationTest extends AbstractWidgetsIntegrationTest {
}
