package ru.ohapegor.widgets;

import org.apache.commons.lang3.RandomUtils;
import ru.ohapegor.widgets.dto.AreaFilter;
import ru.ohapegor.widgets.entity.WidgetEntity;

import java.util.Random;

public class TestObjectsFactory {

    public static WidgetEntity randomWidget() {
        return WidgetEntity.builder()
                .height(randomInt())
                .width(randomInt())
                .x(randomInt())
                .y(randomInt())
                .z(randomInt())
                .build();
    }

    public static int randomInt() {
        return new Random().nextInt(1000);
    }

    public static WidgetEntity randomWidgetOutsideArea(AreaFilter filter) {
        WidgetEntity widget = randomWidget();
        widget.setX(randomIntOutsideRange(filter.getMinX(), filter.getMaxX()));
        widget.setY(randomIntOutsideRange(filter.getMinY(), filter.getMaxY()));
        return widget;
    }

    public static WidgetEntity randomWidgetInsideArea(AreaFilter filter) {
        WidgetEntity widget = randomWidget();
        widget.setX(RandomUtils.nextInt(filter.getMinX()+1, filter.getMaxX()));
        widget.setWidth(RandomUtils.nextInt(1,filter.getMaxX() - widget.getX()));
        widget.setY(RandomUtils.nextInt(filter.getMinY(), filter.getMaxY()));
        widget.setHeight(RandomUtils.nextInt(1,filter.getMaxY() - widget.getY()));
        return widget;
    }

    private static int randomIntOutsideRange(int min, int max) {
        while (true) {
            int randomInt = new Random().nextInt();
            if (randomInt < min || randomInt > max) {
                return randomInt;
            }
        }
    }
}
