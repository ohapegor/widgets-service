package ru.ohapegor.widgets.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.ohapegor.widgets.dto.WidgetDTO;
import ru.ohapegor.widgets.entity.WidgetEntity;

@Mapper(componentModel = "spring")
public interface WidgetsMapper {

    WidgetDTO toDto(WidgetEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    WidgetEntity toNewEntity(WidgetDTO widgetDto);

    @Mapping(source = "widgetId", target = "id")
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    WidgetEntity toUpdatedEntity(WidgetDTO widgetDto, String widgetId);
}
