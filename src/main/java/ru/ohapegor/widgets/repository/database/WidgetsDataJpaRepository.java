package ru.ohapegor.widgets.repository.database;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.ohapegor.widgets.model.WidgetEntity;

import java.util.Optional;

public interface WidgetsDataJpaRepository extends PagingAndSortingRepository<WidgetEntity, String> {

    Optional<WidgetEntity> findByZ(int z);

    boolean existsByZ(int z);

    @Query(value = "SELECT max(z) FROM widgets", nativeQuery = true)
    Integer findMaxZ();

}
