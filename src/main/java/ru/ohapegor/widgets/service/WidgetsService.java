package ru.ohapegor.widgets.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.ohapegor.widgets.config.WidgetServiceProps;
import ru.ohapegor.widgets.exception.OperationTimeoutExceededException;
import ru.ohapegor.widgets.exception.WidgetNotFoundException;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.model.WidgetEntity;
import ru.ohapegor.widgets.repository.WidgetsRepository;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class WidgetsService {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private final WidgetsRepository widgetsRepository;
    private final WidgetServiceProps props;

    @SneakyThrows
    public Optional<WidgetEntity> findById(String id) {
        try {
            if (!readLock.tryLock(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new OperationTimeoutExceededException("findById id = " + id);
            }
            return widgetsRepository.findById(id);
        } finally {
            readLock.unlock();
        }
    }

    @SneakyThrows
    public void deleteById(String id) {
        try {
            if (!writeLock.tryLock(props.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new OperationTimeoutExceededException("deleteById id = " + id);
            }
            widgetsRepository.deleteById(id);
        } finally {
            if (writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }
    }

    @SneakyThrows
    public WidgetEntity create(WidgetEntity widget) {
        try {
            if (!writeLock.tryLock(props.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new OperationTimeoutExceededException("create  widget = " + widget);
            }
            ensureZIndex(widget);
            return widgetsRepository.save(widget);
        } finally {
            if (writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }
    }

    @SneakyThrows
    public WidgetEntity update(WidgetEntity updatedWidget) {
        try {
            if (!writeLock.tryLock(props.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new OperationTimeoutExceededException("update  widget = " + updatedWidget);
            }
            WidgetEntity oldWidget = widgetsRepository.findById(updatedWidget.getId())
                    .orElseThrow(WidgetNotFoundException::new);
            if (updatedWidget.getZ() == null && oldWidget.getZ().equals(widgetsRepository.getMaxZ())) {
                updatedWidget.setZ(oldWidget.getZ());
            } else if (!Objects.equals(oldWidget.getZ(), updatedWidget.getZ())) {
                ensureZIndex(updatedWidget);
            }
            updatedWidget.setCreatedAt(oldWidget.getCreatedAt());
            return widgetsRepository.save(updatedWidget);
        } finally {
            if (writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }
    }

    @SneakyThrows
    public Page<WidgetEntity> getPage(int page, int size, SearchArea filter) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "z"));
        try {
            if (!readLock.tryLock(props.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new OperationTimeoutExceededException("getAll pageable = " + pageable);
            }
            return widgetsRepository.getPage(pageable, filter);
        } finally {
            readLock.unlock();
        }
    }

    private void ensureZIndex(WidgetEntity widget) {
        if (widget.getZ() == null) {
            widget.setZ(nextZ(widgetsRepository.getMaxZ()));
        } else if (widgetsRepository.existsByZ(widget.getZ())) {
            shiftWidgets(widget.getZ());
        }
    }

    private void shiftWidgets(int z) {
        int currentZ = z;
        Optional<WidgetEntity> conflictEntityOpt = widgetsRepository.findByZ(currentZ);
        LinkedList<WidgetEntity> updatedWidgets = new LinkedList<>();
        while (conflictEntityOpt.isPresent()) {
            WidgetEntity conflictEntity = conflictEntityOpt.get();
            currentZ = nextZ(conflictEntity.getZ());
            conflictEntity.setZ(currentZ);
            updatedWidgets.add(conflictEntity);
            conflictEntityOpt = widgetsRepository.findByZ(currentZ);
        }
        widgetsRepository.saveAll(updatedWidgets);
    }

    private int nextZ(int z) {
        if (z == Integer.MAX_VALUE) {
            throw new ArithmeticException("z index overflowed");
        }
        return z + 1;
    }


}
