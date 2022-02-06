package ru.ohapegor.widgets.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.GATEWAY_TIMEOUT, reason = "Operation timeout")
public class OperationTimeoutExceededException extends RuntimeException {
    public OperationTimeoutExceededException(String operationDescription) {
        super("timeout exceeded for operation - " + operationDescription);
    }
}
