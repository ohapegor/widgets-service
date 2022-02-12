package ru.ohapegor.widgets.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchArea {
    private Integer minX;
    private Integer minY;
    private Integer maxX;
    private Integer maxY;

    public boolean isClosed() {
        return minX != null && minY != null && maxX != null && maxY != null;
    }

    public boolean includes(Rectangle rectangle) {
        if (maxX != null && rectangle.getMaxX() > maxX) {
            return false;
        }
        if (maxY != null && rectangle.getMaxY() > maxY) {
            return false;
        }
        if (minX != null && rectangle.getMinX() < minX) {
            return false;
        }
        if (minY != null && rectangle.getMinY() < minY) {
            return false;
        }
        return true;
    }

    public boolean isOverlap(Rectangle rectangle) {
        return isOverlapByX(rectangle) && isOverlapByY(rectangle);
    }

    private boolean isOverlapByX(Rectangle rectangle) {
        return Optional.ofNullable(maxX).map(maxX -> rectangle.getMinX() < maxX).orElse(true) &&
                Optional.ofNullable(minX).map(minX -> rectangle.getMaxX() > minX).orElse(true);
    }

    private boolean isOverlapByY(Rectangle rectangle) {
        return Optional.ofNullable(maxY).map(maxY -> rectangle.getMinY() < maxY).orElse(true) &&
                Optional.ofNullable(minY).map(minY -> rectangle.getMaxY() > minY).orElse(true);
    }
}
