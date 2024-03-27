package uk.ac.ed.acpstorageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static jakarta.servlet.RequestDispatcher.FORWARD_REQUEST_URI;

/**
 * handle global errors with a project specific better message
 */
@Controller
public class ErrorHandler implements ErrorController {

    /**
     * Retrieve some critical error information and display it accordingly
     *
     * @param request the error request which includes the original error
     * @return HTML representation to display for the error
     */

    @RequestMapping("/error")
    @ResponseBody
    public String handleError(HttpServletRequest request) {
        String errorUrl = (String) request.getAttribute(FORWARD_REQUEST_URI);
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
        return String.format("<html><body><h1>ILP-REST-Server - Error Page</h1><div>Status code: <b>%s</b></div>"
                        + "<div>Exception Message: <b>%s</b></div> "
                        + "<div>Original URL: <b>%s</b></div> "
                        + "<br/><div>Timestamp: <b>%s</b></div>"
                        + "<body></html>",
                statusCode, exception==null? "N/A": exception.getMessage(), errorUrl, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()));
    }


}