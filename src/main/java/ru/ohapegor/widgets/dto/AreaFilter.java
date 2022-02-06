package ru.ohapegor.widgets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AreaFilter {
    private Integer minX;
    private Integer minY;
    private Integer maxX;
    private Integer maxY;
}
