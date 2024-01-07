package net.kvibews.exception

import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

data class ErrorDetails(val message: String)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = [(DocumentNotFoundException::class)])
    fun handleDocumentNotFoundException(ex: DocumentNotFoundException): ResponseEntity<ErrorDetails> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorDetails(ex.message!!))
    }
}