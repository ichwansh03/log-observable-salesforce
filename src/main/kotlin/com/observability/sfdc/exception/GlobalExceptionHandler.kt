package com.observability.sfdc.exception

import com.observability.sfdc.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Application exception: {} [{}]", ex.message, ex.code)
        return buildResponse(ex.status, ex.code, ex.message, request.requestURI)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid") }
        logger.warn("Validation failed: {}", errors)
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            request.requestURI,
            errors
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to (violation.message)
        }
        logger.warn("Constraint violation: {}", errors)
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            request.requestURI,
            errors
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: {}", ex.message)
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.message ?: "Invalid argument", request.requestURI)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Conflict: {}", ex.message)
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", ex.message ?: "Conflict", request.requestURI)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: {}", ex.message, ex)
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            request.requestURI
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        code: String,
        message: String,
        path: String,
        details: Map<String, Any>? = null
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                code = code,
                message = message,
                path = path,
                details = details
            )
        )
    }
}
