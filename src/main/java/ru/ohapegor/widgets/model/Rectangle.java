package ru.ohapegor.widgets.model;

import java.math.BigDecimal;

public interface Rectangle {
    int getX();

    int getY();

    int getWidth();

    int getHeight();

    default int getMinX() {
        return getX();
    }

    default int getMinY() {
        return getY();
    }

    default int getMaxX() {
        return getX() + getWidth();
    }

    default int getMaxY() {
        return getY() + getHeight();
    }

    default BigDecimal getArea() {
        return BigDecimal.valueOf(getWidth()).multiply(BigDecimal.valueOf(getHeight()));
    }

    default boolean matchDimensions(Rectangle another) {
        return getX() == another.getX()
                && getMinY() == another.getY()
                && getWidth() == another.getWidth()
                && getHeight() == another.getHeight();
    }
}
