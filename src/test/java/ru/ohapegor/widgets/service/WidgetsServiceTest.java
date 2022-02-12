package ru.ohapegor.widgets.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.ohapegor.widgets.TestObjectsFactory;
import ru.ohapegor.widgets.config.WidgetServiceProps;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.model.WidgetEntity;
import ru.ohapegor.widgets.repository.WidgetsRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetsServiceTest {

    @Mock
    private WidgetsRepository repository;

    @Spy
    private WidgetServiceProps props = new WidgetServiceProps(100, 200, "-");

    @InjectMocks
    private WidgetsService widgetsService;

    @Captor
    private ArgumentCaptor<Iterable<WidgetEntity>> saveWidgetsCaptor;

    private int nOfThreads = 32;
    private ExecutorService executor = Executors.newFixedThreadPool(nOfThreads);


    @Test
    void verifyCreationOfWidgetWithSameZCausesShifting() {
        var testWidget1 = TestObjectsFactory.randomWidget();
        int initialZ = testWidget1.getZ();
        var testWidget2 = TestObjectsFactory.randomWidget();
        testWidget2.setZ(initialZ);

        // doAnswer(new AnswersWithDelay(1000, new Returns("some-return-value"))).when(repository).save(any());
        when(repository.findByZ(testWidget1.getZ())).thenReturn(Optional.of(testWidget1));
        when(repository.existsByZ(initialZ)).thenReturn(true);

        widgetsService.create(testWidget2);

        verify(repository, times(1)).saveAll(saveWidgetsCaptor.capture());
        Iterator<WidgetEntity> capturedIterator = saveWidgetsCaptor.getValue().iterator();
        WidgetEntity captured = capturedIterator.next();
        assertFalse(capturedIterator.hasNext());
        assertEquals(initialZ + 1, captured.getZ());
    }

    @Test
    void verifyAllReadOperationsArePerformedInParallel() throws ExecutionException, InterruptedException {
        int readDelayMS = 1000;

        doAnswer(new AnswersWithDelay(readDelayMS, new Returns(Optional.empty())))
                .when(repository).findById(any());
        doAnswer(new AnswersWithDelay(readDelayMS, new Returns(new PageImpl<>(emptyList(), Pageable.unpaged(), 0))))
                .when(repository).getPage(any(), any());

        Instant start = Instant.now();
        var futureList = IntStream.range(0, nOfThreads)
                .boxed()
                .map(i -> i % 2 == 0 ? submitGetByIdTask() : submitGetPageTask())
                .collect(Collectors.toList());

        for (Future<Boolean> future : futureList) {
            assertTrue(future.get());
        }

        var executionDuration = Duration.between(Instant.now(), start);
        var doubleReadDelayDuration = Duration.ofMillis(readDelayMS * 2);
        //verify that 32 reads with 32 threads execution time took less than 2 read delays
        assertEquals(1,doubleReadDelayDuration.compareTo(executionDuration));
    }

    private Future<Boolean> submitGetByIdTask() {
        String id = UUID.randomUUID().toString();
        return executor.submit(() -> widgetsService.findById(id).isEmpty());
    }

    private Future<Boolean> submitGetPageTask() {
        return executor.submit(() -> widgetsService.getPage(0, 10, new SearchArea()).isEmpty());
    }

}