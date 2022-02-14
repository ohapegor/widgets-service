package ru.ohapegor.widgets.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.GATEWAY_TIMEOUT, reason = "Operation lock timeout")
public class OperationLockTimeoutExceededException extends RuntimeException {
    public OperationLockTimeoutExceededException(String operationDescription) {
        super("waiting lock timeout exceeded for operation - " + operationDescription);
    }
}
