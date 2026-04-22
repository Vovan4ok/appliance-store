package com.vovan4ok.appliance.store.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                               HttpServletRequest request,
                                               Model model) {
        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        model.addAttribute("status", 409);
        model.addAttribute("error", "Conflict");
        model.addAttribute("message", "Cannot complete this operation because the record is still referenced by other data. Remove the related records first.");
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleIllegalArgument(IllegalArgumentException ex,
                                        HttpServletRequest request,
                                        Model model) {
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         Model model) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "An unexpected error occurred. Please try again later.");
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }
}
