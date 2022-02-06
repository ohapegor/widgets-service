package ru.ohapegor.widgets.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Widget not found by id")
public class WidgetNotFoundException extends RuntimeException {
}
