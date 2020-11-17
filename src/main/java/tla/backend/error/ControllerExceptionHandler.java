package tla.backend.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import tla.error.ObjectNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseBody
    public ResponseEntity<?> objectNotFound(ObjectNotFoundException e) {
        log.error("object not found: {}", e.getMessage());
        return new ResponseEntity<>(e, HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseBody
    public String unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        log.error("media type not supported: ", e.getMessage());
        return String.format(
            "sry, media type %s is not supported",
            e.getContentType()
        );
    }

    @ExceptionHandler(Exception.class)
    public void anyException(Exception e) {
        log.error("exception happened.", e);
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "something went wrong while doing something.",
            e
        );
    }

}
