package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "widgets.repository=in-memory")
class InMemoryRepositoryIntegrationTest extends AbstractWidgetsIntegrationTest{


}
