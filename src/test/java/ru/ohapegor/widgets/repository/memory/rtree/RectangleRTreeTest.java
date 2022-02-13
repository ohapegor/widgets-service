package ru.ohapegor.widgets.repository.memory.rtree;

import org.junit.jupiter.api.Test;
import ru.ohapegor.widgets.TestObjectsFactory;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.model.WidgetEntity;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectangleRTreeTest {

    private RectangleRTree<WidgetEntity> tree = new RectangleRTree<>(2, 5);

    @Test
    void verifySearchReturnsValuesInsideArea() {
        //prepare test data
        var searchArea = SearchArea.builder()
                .minX(-10)
                .maxX(10)
                .minY(-10)
                .maxY(10)
                .build();

        var widgetsInsideArea = Stream.of(
                TestObjectsFactory.randomWidgetWithCoords(-5, -5, 5, 5),
                TestObjectsFactory.randomWidgetWithCoords(-10, -10, 10, 10),
                TestObjectsFactory.randomWidgetWithCoords(0, -5, 5, 5),
                TestObjectsFactory.randomWidgetWithCoords(-5, 0, 5, 5),
                TestObjectsFactory.randomWidgetWithCoords(-5, -5, 0, 5),
                TestObjectsFactory.randomWidgetWithCoords(-5, -5, 5, 0),
                TestObjectsFactory.randomWidgetWithCoords(5, -9, 9, -5),
                TestObjectsFactory.randomWidgetWithCoords(0, 8, 9, 10)
        ).collect(Collectors.toMap(WidgetEntity::getId, Function.identity()));

        var outsideArea = Stream.of(
                TestObjectsFactory.randomWidgetWithCoords(-10, -10, 10, 11),
                TestObjectsFactory.randomWidgetWithCoords(-30, -5, 5, 5),
                TestObjectsFactory.randomWidgetWithCoords(-25, -25, -15, -15),
                TestObjectsFactory.randomWidgetWithCoords(15, 15, 25, 25)
        ).collect(Collectors.toMap(WidgetEntity::getId, Function.identity()));

        //insert all widgets in r-tree
        Stream.concat(
                widgetsInsideArea.values().stream(),
                outsideArea.values().stream()
        ).forEach(w -> {
            var node = new EntryNode<>(w);
            node.setDimensions(w);
            tree.insert(node);
        });

        //verify search widgets returns correct result
        List<WidgetEntity> foundByArea = tree.search(searchArea);
        assertEquals(widgetsInsideArea.size(), foundByArea.size());
        foundByArea.forEach(widget -> assertTrue(widgetsInsideArea.containsKey(widget.getId())));

        var widgetsToDelete = widgetsInsideArea.values().stream()
                .limit(widgetsInsideArea.size() / 2)
                .collect(Collectors.toMap(WidgetEntity::getId, Function.identity()));

        //verify search widgets returns correct result after deletion of half widgets inside area
        widgetsToDelete.forEach((id, widget) -> {
                    tree.deleteEntry(id, widget);
                    widgetsInsideArea.remove(id);
                }
        );
        foundByArea = tree.search(searchArea);
        assertEquals(widgetsInsideArea.size(), foundByArea.size());
        foundByArea.forEach(widget -> assertTrue(widgetsInsideArea.containsKey(widget.getId())));
    }

}