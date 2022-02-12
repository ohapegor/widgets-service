package ru.ohapegor.widgets.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ohapegor.widgets.model.SearchArea;
import ru.ohapegor.widgets.dto.WidgetDTO;
import ru.ohapegor.widgets.model.WidgetEntity;
import ru.ohapegor.widgets.mapper.WidgetsMapper;
import ru.ohapegor.widgets.service.WidgetsService;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/api/v1/widgets")
@RequiredArgsConstructor
@Validated
public class WidgetsController {

    private static final String TOTAL_COUNT_HEADER = "x-total-count";
    private static final String CURRENT_PAGE_HEADER = "x-page-number";
    private static final String PAGE_SIZE_HEADER = "x-page-size";


    private final WidgetsService service;
    private final WidgetsMapper mapper;

    @Operation(summary = "Get widget by id")
    @ApiResponse(responseCode = "200", description = "widget is found")
    @ApiResponse(
            responseCode = "404",
            description = "widget is not found",
            content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))}
    )
    @GetMapping(
            path = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WidgetDTO> findById(@PathVariable String id) {
        log.debug("requested widget by id = {}", id);
        return service.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get a list of widgets with pagination and filtered by area")
    @ApiResponse(
            responseCode = "200",
            description = "widgets list in response",
            content = {@Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = WidgetDTO.class)))}
    )
    @ApiResponse(
            responseCode = "400",
            description = "request parameters are not valid",
            content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))}
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findPage(
            @Valid @Min(0) @RequestParam(required = false, defaultValue = "0") int page,
            @Valid @Min(0) @Max(500) @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) Integer minX,
            @RequestParam(required = false) Integer minY,
            @RequestParam(required = false) Integer maxX,
            @RequestParam(required = false) Integer maxY
    ) {
        var areaFilter = SearchArea.builder()
                .minX(minX).minY(minY).maxX(maxX).maxY(maxY)
                .build();

        log.debug("requested widgets page={}, size={}, with filter={}", page, size, areaFilter);

        if (!isValid(areaFilter)) {
            return buildBadRequest("invalid area filter : " + areaFilter);
        }

        Page<WidgetDTO> widgetDTOPage = service.getPage(page, size, areaFilter)
                .map(mapper::toDto);

        return ResponseEntity.status(HttpStatus.OK)
                .header(TOTAL_COUNT_HEADER, String.valueOf(widgetDTOPage.getTotalElements()))
                .header(CURRENT_PAGE_HEADER, String.valueOf(page))
                .header(PAGE_SIZE_HEADER, String.valueOf(size))
                .body(widgetDTOPage.getContent());
    }

    @Operation(summary = "Crate new widget")
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WidgetDTO> createWidget(@Valid @RequestBody WidgetDTO widgetDTO) {
        log.debug("requested creation of widget with body {}", widgetDTO);
        WidgetEntity createdWidget = service.create(mapper.toNewEntity(widgetDTO));
        WidgetDTO createdWidgetDTO = mapper.toDto(createdWidget);
        log.debug("widget created : {}", createdWidgetDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWidgetDTO);
    }

    @Operation(summary = "Update widget by id")
    @ApiResponse(responseCode = "200", description = "widget is updated")
    @ApiResponse(
            responseCode = "400",
            description = "request is invalid",
            content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))}
    )
    @ApiResponse(
            responseCode = "404",
            description = "widget is not found",
            content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))}
    )
    @PutMapping(
            path = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WidgetDTO updateWidget(@PathVariable String id, @Valid @RequestBody WidgetDTO widgetDTO) {
        log.debug("requested update of widget with id = {} and body {}", id, widgetDTO);
        widgetDTO.setId(id);
        WidgetEntity createdWidget = service.update(mapper.toUpdatedEntity(widgetDTO, id));
        return mapper.toDto(createdWidget);
    }

    @Operation(summary = "Delete widget by id")
    @ApiResponse(responseCode = "204", description = "widget is deleted")
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.debug("requested deletion of widget with id = {}", id);
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isValid(SearchArea areaFilter) {
        if (areaFilter.getMinX() != null && areaFilter.getMaxX() != null && areaFilter.getMaxX() < areaFilter.getMinX()) {
            return false;
        }
        if (areaFilter.getMinY() != null && areaFilter.getMaxY() != null && areaFilter.getMaxY() < areaFilter.getMinY()) {
            return false;
        }
        return true;
    }

    private ResponseEntity<ApiError> buildBadRequest(String errorMessage) {
        return ResponseEntity.badRequest().body(new ApiError(HttpStatus.BAD_REQUEST, errorMessage));
    }
}
