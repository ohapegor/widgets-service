package ru.ohapegor.widgets.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchAreaTest {

    @Test
    void isOverlap1() {
        SearchArea s = new SearchArea(1, 1, 3, 3);
        Rectangle w = WidgetEntity.builder()
                .x(0)
                .y(0)
                .width(5)
                .height(5)
                .build();
        assertTrue(s.isOverlap(w));
    }

    @Test
    void isOverlap2() {
        SearchArea s = new SearchArea(0, 0, 5, 5);
        Rectangle w = WidgetEntity.builder()
                .x(1)
                .y(1)
                .width(2)
                .height(2)
                .build();
        assertTrue(s.isOverlap(w));
    }

    @Test
    void isOverlap3() {
        SearchArea s = new SearchArea(1, 1, 3, 3);
        Rectangle w = WidgetEntity.builder()
                .x(5)
                .y(5)
                .width(5)
                .height(5)
                .build();
        assertFalse(s.isOverlap(w));
    }

    @Test
    void isOverlap4() {
        SearchArea s = new SearchArea(0, 0, 5, 5);
        Rectangle w = WidgetEntity.builder()
                .x(1)
                .y(1)
                .width(5)
                .height(5)
                .build();
        assertTrue(s.isOverlap(w));
    }

    @Test
    void isOverlap5() {
        SearchArea s = new SearchArea(0, 0, 5, 5);
        Rectangle w = WidgetEntity.builder()
                .x(-1)
                .y(-1)
                .width(4)
                .height(4)
                .build();
        assertTrue(s.isOverlap(w));
    }
}