package ru.ohapegor.widgets.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.ohapegor.widgets.dto.AreaFilter;
import ru.ohapegor.widgets.entity.WidgetEntity;

import java.util.Optional;

public interface WidgetsRepository {

    Optional<WidgetEntity> findById(String id);

    WidgetEntity save(WidgetEntity entity);

    void deleteById(String id);

    boolean existsByZ(int z);

    Optional<WidgetEntity> findByZ(int z);

    Integer getMaxZ();

    void deleteAll();

    Page<WidgetEntity> getPage(Pageable pageable, AreaFilter filter);

    long count();

    void saveAll(Iterable<WidgetEntity> updatedWidgets);
}
