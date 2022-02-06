package ru.ohapegor.mirotest.repository.memory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.ohapegor.mirotest.dto.AreaFilter;
import ru.ohapegor.mirotest.entity.WidgetEntity;
import ru.ohapegor.mirotest.repository.WidgetsRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(value = "widgets.repository", havingValue = "in-memory")
public class InMemoryWidgetsRepository implements WidgetsRepository {

    private final Map<String, WidgetEntity> widgetsById = new ConcurrentHashMap<>();

    private final NavigableMap<Integer, WidgetEntity> widgetsByZ = new ConcurrentSkipListMap<>();

    @Override
    public Optional<WidgetEntity> findById(String id) {
        return Optional.ofNullable(widgetsById.get(id));
    }

    @Override
    public WidgetEntity save(WidgetEntity entity) {
        if (entity.getZ() == null) {
            throw new IllegalStateException("z number should not be null");
        }
        String id = entity.getId();
        if (id == null) {
            id = generateId();
            entity.setId(id);
        }
        entity.setLastModifiedAt(Instant.now());
        widgetsById.put(id, entity);
        widgetsByZ.put(entity.getZ(), entity);
        return entity;
    }

    @Override
    public void deleteById(String id) {
        WidgetEntity widgetEntity = widgetsById.remove(id);
        if (widgetEntity != null) {
            widgetsByZ.remove(widgetEntity.getZ());
        }
    }

    @Override
    public boolean existsByZ(int z) {
        return widgetsByZ.containsKey(z);
    }

    @Override
    public Optional<WidgetEntity> findByZ(int z) {
        return Optional.ofNullable(widgetsByZ.get(z));
    }

    @Override
    public Integer getMaxZ() {
        return widgetsByZ.isEmpty() ? 0 : widgetsByZ.lastKey();
    }

    @Override
    @Transient
    public void deleteAll() {
        widgetsById.clear();
        widgetsByZ.clear();
    }

    @Override
    public Page<WidgetEntity> getPage(Pageable pageable, AreaFilter filter) {
        List<WidgetEntity> widgetsMatchFilter = widgetsByZ.values()
                .stream()
                .filter(widget -> matchArea(widget, filter))
                .collect(Collectors.toList());

        List<WidgetEntity> widgetsInPage = widgetsMatchFilter
                .stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        return new PageImpl<>(widgetsInPage, pageable, widgetsMatchFilter.size());
    }

    @Override
    public long count() {
        return widgetsById.size();
    }

    @Override
    public void saveAll(Iterable<WidgetEntity> updatedWidgets) {
        if (updatedWidgets != null) {
            updatedWidgets.forEach(this::save);
        }
    }

    private boolean matchArea(WidgetEntity widget, AreaFilter areaFilter) {
        if (areaFilter == null) {
            return true;
        }
        if (areaFilter.getMaxX() != null && widget.getX() + widget.getWidth() > areaFilter.getMaxX()) {
            return false;
        }
        if (areaFilter.getMaxY() != null && widget.getY() + widget.getHeight() > areaFilter.getMaxY()) {
            return false;
        }
        if (areaFilter.getMinX() != null && widget.getX() < areaFilter.getMinX()) {
            return false;
        }
        if (areaFilter.getMinY() != null && widget.getY() < areaFilter.getMinY()) {
            return false;
        }
        return true;
    }

    private String generateId() {
        while (true) { //check possible collision
            String id = UUID.randomUUID().toString();
            if (!widgetsById.containsKey(id)) {
                return id;
            }
        }
    }


}
