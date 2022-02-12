package ru.ohapegor.widgets.repository.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.model.WidgetEntity;
import ru.ohapegor.widgets.repository.WidgetsRepository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Transactional
@Slf4j
public class WidgetsH2Repository implements WidgetsRepository {

    private final WidgetsDataJpaRepository repository;
    private final EntityManager entityManager;

    public WidgetsH2Repository(WidgetsDataJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<WidgetEntity> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public WidgetEntity save(WidgetEntity entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByZ(int z) {
        return repository.existsByZ(z);
    }

    @Override
    public Optional<WidgetEntity> findByZ(int z) {
        return repository.findByZ(z);
    }

    @Override
    public Integer getMaxZ() {
        return Optional.ofNullable(repository.findMaxZ()).orElse(0);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public Page<WidgetEntity> getPage(Pageable pageable, SearchArea filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<WidgetEntity> widgetsQuery = cb.createQuery(WidgetEntity.class);
        CriteriaQuery<Long> totalQ = cb.createQuery(Long.class);

        Root<WidgetEntity> fromW = widgetsQuery.from(WidgetEntity.class);
        Root<WidgetEntity> fromT = totalQ.from(WidgetEntity.class);

        CriteriaQuery<WidgetEntity> selectW = widgetsQuery.select(fromW);
        CriteriaQuery<Long> selectT = totalQ.select(cb.count(fromT));

        if (filter != null) {
            List<Predicate> predicatesW = new ArrayList<>();
            List<Predicate> predicatesT = new ArrayList<>();

            if (filter.getMinX() != null) {
                predicatesW.add(cb.ge(fromW.get("x"), filter.getMinX()));
                predicatesT.add(cb.ge(fromT.get("x"), filter.getMinX()));
            }
            if (filter.getMinY() != null) {
                predicatesW.add(cb.ge(fromW.get("y"), filter.getMinY()));
                predicatesT.add(cb.ge(fromT.get("y"), filter.getMinY()));
            }
            if (filter.getMaxX() != null) {
                predicatesW.add(cb.le(cb.sum(fromW.get("x"), fromW.get("width")), filter.getMaxX()));
                predicatesT.add(cb.le(cb.sum(fromT.get("x"), fromT.get("width")), filter.getMaxX()));
            }
            if (filter.getMaxY() != null) {
                predicatesW.add(cb.le(cb.sum(fromW.get("y"), fromW.get("height")), filter.getMaxY()));
                predicatesT.add(cb.le(cb.sum(fromT.get("y"), fromT.get("height")), filter.getMaxY()));
            }
            if (!predicatesW.isEmpty()) {
                selectW = selectW.where(cb.and(predicatesW.toArray(new Predicate[0])));
            }
            if (!predicatesT.isEmpty()) {
                selectT = selectT.where(cb.and(predicatesT.toArray(new Predicate[0])));
            }
        }

        selectW = selectW.orderBy(cb.asc(fromW.get("z")));
        TypedQuery<WidgetEntity> typedQW = entityManager.createQuery(selectW);
        typedQW.setFirstResult((int) pageable.getOffset());
        typedQW.setMaxResults(pageable.getPageSize());

        Long totalCount = entityManager.createQuery(selectT).getSingleResult();
        return new PageImpl<>(typedQW.getResultList(), pageable, totalCount);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void saveAll(Iterable<WidgetEntity> updatedWidgets) {
        repository.saveAll(updatedWidgets);
    }

}
