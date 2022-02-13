package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "widgets.repository=h2")
class H2RepositoryIntegrationTest extends AbstractWidgetsIntegrationTest {
}
