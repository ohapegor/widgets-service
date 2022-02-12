package ru.ohapegor.widgets.utils;

import ru.ohapegor.widgets.model.WidgetEntity;

import java.util.Objects;

public abstract class WidgetUtils {
    public static boolean isZIndexModified(WidgetEntity oldEntity, WidgetEntity newEntity) {
        return !Objects.equals(oldEntity.getZ(), newEntity.getZ());
    }

    public static boolean isSpacialIndexModified(WidgetEntity oldEntity, WidgetEntity newEntity) {
        return !oldEntity.matchDimensions(newEntity);
    }
}
