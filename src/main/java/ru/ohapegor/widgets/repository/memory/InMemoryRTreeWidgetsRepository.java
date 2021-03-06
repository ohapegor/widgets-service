package ru.ohapegor.widgets.repository.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.model.WidgetEntity;
import ru.ohapegor.widgets.repository.WidgetsRepository;
import ru.ohapegor.widgets.repository.memory.rtree.EntryNode;
import ru.ohapegor.widgets.repository.memory.rtree.RectangleRTree;
import ru.ohapegor.widgets.utils.WidgetUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryRTreeWidgetsRepository implements WidgetsRepository {

    private final Map<String, WidgetEntity> widgetsById = new HashMap<>();

    private final NavigableMap<Integer, WidgetEntity> widgetsByZ = new TreeMap<>();

    private final RectangleRTree<WidgetEntity> spatialIndex = new RectangleRTree<>(2, 50);

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
        WidgetEntity oldEntity = null;
        if (id == null) {
            id = generateId();
            entity.setId(id);
        } else {
            oldEntity = widgetsById.get(id);
        }
        entity.setLastModifiedAt(Instant.now());
        if (oldEntity != null) {
            /* if corresponding index not changed we can just update data in entity by reference
             * and skip inserting entity which would cause expensive redistribution of elements inside indexing trees
             */
            boolean spacialIndexIsModified = WidgetUtils.isSpacialIndexModified(oldEntity, entity);
            boolean zIndexModified = WidgetUtils.isZIndexModified(oldEntity, entity);
            // check additionally if entity in z index was overridden with by shifting
            boolean zIndexNotOverridden = Objects.equals(widgetsByZ.get(oldEntity.getZ()).getId(), oldEntity.getId());
            if (zIndexModified && zIndexNotOverridden) {
                widgetsByZ.remove(oldEntity.getZ());
            }
            if (spacialIndexIsModified) {
                spatialIndex.deleteEntry(oldEntity.getId(), oldEntity);
            }
            oldEntity.updateData(entity);
            if (zIndexModified) {
                widgetsByZ.put(oldEntity.getZ(), oldEntity);
            }
            if (spacialIndexIsModified) {
                var entryNode = new EntryNode<>(entity);
                entryNode.setDimensions(entity);
                spatialIndex.insert(entryNode);
            }
        } else {
            widgetsById.put(entity.getId(), entity);
            widgetsByZ.put(entity.getZ(), entity);
            var entryNode = new EntryNode<>(entity);
            entryNode.setDimensions(entity);
            spatialIndex.insert(entryNode);
        }
        return entity.clone();
    }


    @Override
    public void deleteById(String id) {
        WidgetEntity widgetEntity = widgetsById.remove(id);
        if (widgetEntity != null) {
            widgetsByZ.remove(widgetEntity.getZ());
            spatialIndex.deleteEntry(id, widgetEntity);
        }
    }

    @Override
    public boolean existsByZ(int z) {
        return widgetsByZ.containsKey(z);
    }

    @Override
    public Optional<WidgetEntity> findByZ(int z) {
        return Optional.ofNullable(widgetsByZ.get(z)).map(WidgetEntity::clone);
    }

    @Override
    public Integer getMaxZ() {
        return widgetsByZ.isEmpty() ? 0 : widgetsByZ.lastKey();
    }

    @Override
    @Transient
    public void deleteAll() {
        spatialIndex.clear();
        widgetsById.clear();
        widgetsByZ.clear();
    }

    @Override
    public Page<WidgetEntity> getPage(Pageable pageable, SearchArea searchArea) {
        List<WidgetEntity> widgetsMatchFilter = spatialIndex.search(searchArea);

        int size = widgetsMatchFilter.size();
        List<WidgetEntity> widgetsInPage = widgetsMatchFilter
                .stream()
                .sorted(Comparator.comparing(WidgetEntity::getZ))
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(WidgetEntity::clone)
                .collect(Collectors.toList());

        return new PageImpl<>(widgetsInPage, pageable, size);
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

    private String generateId() {
        while (true) { //check possible collision
            String id = UUID.randomUUID().toString();
            if (!widgetsById.containsKey(id)) {
                return id;
            }
        }
    }


}
