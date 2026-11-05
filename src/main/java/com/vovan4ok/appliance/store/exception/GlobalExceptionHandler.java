package com.vovan4ok.appliance.store.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.ui.Model;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.json.JacksonJsonView;

import java.time.LocalDateTime;

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
        model.addAttribute("message",
                "Cannot complete this operation because the record is still referenced by other data."
                + " Remove the related records first.");
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

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFound() {
        // static resource missing (e.g. favicon.ico) — return 404 with no body and no log noise
    }

    /**
     * Spring's own request errors (wrong method 405, unsupported media type 415, bad parameter or body 400...)
     * keep their status instead of falling into the 500 handler below. A wrong method fails before any controller
     * is chosen, so REST calls land here too: those get the API's JSON error format instead of the HTML page.
     */
    @ExceptionHandler({ServletException.class, ErrorResponseException.class,
            TypeMismatchException.class, HttpMessageNotReadableException.class})
    public ModelAndView handleRequestError(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatusCode status = RequestErrors.status(ex);
        String message;
        if (status.is5xxServerError()) {
            log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
            message = "An unexpected error occurred. Please try again later.";
        } else {
            log.warn("{} {} {}: {}", status.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
            message = RequestErrors.message(ex);
        }
        RequestErrors.headers(ex).forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));

        boolean api = request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        ModelAndView view = api ? new ModelAndView(new JacksonJsonView()) : new ModelAndView("error");
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("error", RequestErrors.reason(status));
        view.addObject("message", message);
        view.addObject("path", request.getRequestURI());
        if (api) {
            view.addObject("timestamp", LocalDateTime.now());
        }
        return view;
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
