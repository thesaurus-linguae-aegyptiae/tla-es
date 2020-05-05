package tla.backend.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseBody
    public String objectNotFound(ObjectNotFoundException e) {
        log.error("object not found: {}", e.getMessage());
        return "sry, object not found.";
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
}