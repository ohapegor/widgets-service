package ru.ohapegor.widgets.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Instant;

@Data
public class WidgetDTO {

    private String id;

    @NotNull
    @Positive
    private Integer height;

    @NotNull
    @Positive
    private Integer width;

    @NotNull
    private Integer x;

    @NotNull
    private Integer y;

    private Integer z;

    private Instant createdAt;

    private Instant lastModifiedAt;

}
