package com.vovan4ok.appliance.store.exception;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;

/**
 * Status, headers and a safe message for Spring's own request errors: wrong HTTP method (405), unsupported
 * content type (415), missing or invalid parameters (400), malformed request body (400)... They are the client's
 * fault, so the catch-all handlers must not turn them into a 500.
 */
final class RequestErrors {

    private RequestErrors() {
    }

    static HttpStatusCode status(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode();
        }
        if (ex instanceof TypeMismatchException || ex instanceof HttpMessageNotReadableException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** E.g. {@code Allow: GET, PUT, DELETE} for a 405, as HTTP requires. */
    static HttpHeaders headers(Exception ex) {
        return ex instanceof ErrorResponse errorResponse ? errorResponse.getHeaders() : HttpHeaders.EMPTY;
    }

    static String reason(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Error";
    }

    /** Short description for the client; never internal details like class names or stack traces. */
    static String message(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
            return errorResponse.getBody().getDetail();
        }
        if (ex instanceof TypeMismatchException mismatch && mismatch.getPropertyName() != null) {
            return "Invalid value for '" + mismatch.getPropertyName() + "'";
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed request body";
        }
        return "Invalid request";
    }
}
