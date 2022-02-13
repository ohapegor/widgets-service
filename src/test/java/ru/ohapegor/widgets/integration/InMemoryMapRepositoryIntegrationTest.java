package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "widgets.repository=maps")
class InMemoryMapRepositoryIntegrationTest extends AbstractWidgetsIntegrationTest{
}
