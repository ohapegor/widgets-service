package ru.ohapegor.widgets.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.ohapegor.widgets.TestObjectsFactory;
import ru.ohapegor.widgets.dto.WidgetDTO;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.model.WidgetEntity;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.service.WidgetsService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
abstract class AbstractWidgetsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WidgetsRepository repository;

    @Autowired
    private WidgetsService service;

    @Autowired
    private ObjectMapper om;

    @AfterEach
    void teardown() {
        repository.deleteAll();
    }

    @Test
    void verifyGetByIdReturnsWidget() throws Exception {
        WidgetEntity widgetEntity = repository.save(TestObjectsFactory.randomWidget());
        mockMvc.perform(get("/api/v1/widgets/" + widgetEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(widgetEntity.getId()))
                .andExpect(jsonPath("$.x").value(widgetEntity.getX()))
                .andExpect(jsonPath("$.y").value(widgetEntity.getY()))
                .andExpect(jsonPath("$.z").value(widgetEntity.getZ()))
                .andExpect(jsonPath("$.width").value(widgetEntity.getWidth()))
                .andExpect(jsonPath("$.height").value(widgetEntity.getHeight()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.lastModifiedAt").isNotEmpty());
    }

    @Test
    void verifyGetByIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/widgets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyGetPageOfWidgetsReturnsThemSortedByZ() throws Exception {
        int widgetsCount = 20;
        //generate a bunch of widgets with unique Z
        Stream.generate(TestObjectsFactory::randomWidget)
                .limit(widgetsCount)
                .forEach(service::create);

        var pageResult = mockMvc.perform(get("/api/v1/widgets?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(header().string("x-total-count", String.valueOf(widgetsCount)))
                .andExpect(header().string("x-page-number", "1"))
                .andExpect(header().string("x-page-size", "10"))
                .andReturn();

        var widgetsList = getWidgetsListFromResult(pageResult);
        assertEquals(10, widgetsList.size());
        int currentZ = Integer.MIN_VALUE;

        for (WidgetDTO widgetDTO : widgetsList) {
            assertTrue(widgetDTO.getZ() > currentZ,
                    String.format("expected for widget=%s to have z greater than %d", widgetDTO, currentZ)
            );
            currentZ = widgetDTO.getZ();
        }
    }

    @Test
    void verifyWidgetCanBeCreated() throws Exception {
        mockMvc.perform(post("/api/v1/widgets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"height\":1,\"width\":2,\"x\":3,\"y\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.height").value("1"))
                .andExpect(jsonPath("$.width").value("2"))
                .andExpect(jsonPath("$.x").value("3"))
                .andExpect(jsonPath("$.y").value("4"))
                .andExpect(jsonPath("$.z").value("1"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.lastModifiedAt").isNotEmpty());
    }

    @Test
    void verifyCreationOfWidgetWithInvalidParamsRefusedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/widgets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"height\":-1,\"width\":-2,\"x\":-3,\"y\":-4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyWidgetCanBeUpdated() throws Exception {
        var testWidget = persistedRandomWidget();
        var updatedWidget = TestObjectsFactory.randomWidget();
        mockMvc.perform(put("/api/v1/widgets/" + testWidget.getId())
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(updatedWidget)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testWidget.getId()))
                .andExpect(jsonPath("$.height").value(updatedWidget.getHeight()))
                .andExpect(jsonPath("$.width").value(updatedWidget.getWidth()))
                .andExpect(jsonPath("$.x").value(updatedWidget.getX()))
                .andExpect(jsonPath("$.y").value(updatedWidget.getY()))
                .andExpect(jsonPath("$.z").value(updatedWidget.getZ()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.lastModifiedAt").isNotEmpty());

        var page = repository.getPage(PageRequest.of(0, 10), new SearchArea());
        assertEquals(1, page.getTotalElements());
        assertEquals(testWidget.getId(), page.iterator().next().getId());
    }


    @Test
    void verifyUpdateOfWidgetWithInvalidParamsRefusedWithBadRequest() throws Exception {
        var testWidget = persistedRandomWidget();
        mockMvc.perform(put("/api/v1/widgets/" + testWidget.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"height\":0,\"width\":0,\"x\":0,\"y\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyUpdateByIdReturns404() throws Exception {
        mockMvc.perform(put("/api/v1/widgets/" + UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(TestObjectsFactory.randomWidget())))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyWidgetCanBeDeleted() throws Exception {
        var testWidget1 = persistedRandomWidget();
        var testWidget2 = persistedRandomWidget();

        assertEquals(2, repository.count());

        mockMvc.perform(delete("/api/v1/widgets/" + testWidget1.getId()))
                .andExpect(status().isNoContent());

        assertEquals(1, repository.count());
        assertTrue(repository.findById(testWidget1.getId()).isEmpty());
        assertTrue(repository.findById(testWidget2.getId()).isPresent());

        mockMvc.perform(delete("/api/v1/widgets/" + testWidget2.getId()))
                .andExpect(status().isNoContent());

        assertEquals(0, repository.count());
    }

    @Test
    void verifySpacialFilterWorksCorrect() throws Exception {
        /* H2 repository is very slow compared in memory implementations, do no test high counts with it.
           To see search performance increase in search when switching from simple maps implementation with
           linear search complexity to r-tree index with logarithmic search complexity better to set higher counts.
           On my personal machine is set:
           insideCount = 500 (creation took PT13.4689993S | search time took PT0.1079679S)
           outsideCount = 10000 (creation took PT18.0180287S | search took PT0.0190027S)
           Spacial search performance increased more than 5 times, but in cost of longer creation.
         */
        int insideCount = 100;
        int outsideCount = 200;

        var searchArea = new SearchArea(0, 0, 1000, 1000);

        Instant createStart = Instant.now();
        var insideAreaWidgetsIds = Stream.generate(() -> TestObjectsFactory.randomWidgetInsideArea(searchArea))
                .limit(insideCount)
                .map(service::create)
                .map(WidgetEntity::getId)
                .collect(Collectors.toSet());

        Stream.generate(() -> TestObjectsFactory.randomWidgetOutsideArea(searchArea))
                .limit(outsideCount)
                .forEach(service::create);

        Instant createEnd = Instant.now();
        System.out.println("creation took " + Duration.between(createStart, createEnd));


        assertEquals(insideCount + outsideCount, repository.count());

        Instant searchStart = Instant.now();
        var pageResult = mockMvc.perform(get("/api/v1/widgets?size=" + insideCount)
                        .queryParam("minX", String.valueOf(searchArea.getMinX()))
                        .queryParam("maxX", String.valueOf(searchArea.getMaxX()))
                        .queryParam("minY", String.valueOf(searchArea.getMinY()))
                        .queryParam("maxY", String.valueOf(searchArea.getMaxY()))
                )
                .andExpect(status().isOk())
                .andExpect(header().string("x-total-count", String.valueOf(insideCount)))
                .andExpect(header().string("x-page-number", "0"))
                .andExpect(header().string("x-page-size", String.valueOf(insideCount)))
                .andReturn();
        Instant searchEnd = Instant.now();
        System.out.println("search took " + Duration.between(searchStart, searchEnd));

        var widgetsList = getWidgetsListFromResult(pageResult);
        assertEquals(insideCount, widgetsList.size());
        widgetsList.forEach(w -> assertTrue(insideAreaWidgetsIds.contains(w.getId())));
    }

    private WidgetEntity persistedRandomWidget() {
        return repository.save(TestObjectsFactory.randomWidget());
    }

    @SneakyThrows
    private List<WidgetDTO> getWidgetsListFromResult(MvcResult result) {
        return om.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }
}
