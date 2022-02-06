package ru.ohapegor.widgets.integration;

import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = "widgets.repository=h2")
class H2RepositoryIntegrationTest extends AbstractWidgetsIntegrationTest {
}
