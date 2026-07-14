package com.openbankproject.hydra.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Without this, a request that fails Spring's @PostMapping(params=...) matching
 * (e.g. a consent form submitted with no permission checkbox selected) never reaches
 * a controller method, so none of the per-endpoint try/catch blocks in IndexController
 * run. It falls through to the default error view with a blank message instead.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${obp.base_url:#}")
    private String obpBaseUrl;
    @Value("${logo.bank.enabled:false}")
    private String showBankLogo;
    @Value("${logo.bank.url:#}")
    private String bankLogoUrl;
    @Value("${show_unhandled_errors:false}")
    private boolean showUnhandledErrors;

    @ExceptionHandler(UnsatisfiedServletRequestParameterException.class)
    public String handleUnsatisfiedServletRequestParameter(UnsatisfiedServletRequestParameterException e, Model model) {
        logger.warn("Form submission rejected: {}", e.getMessage());
        addCommonAttributes(model);
        model.addAttribute("errorMsg", "The form is missing one or more required fields (" + e.getMessage()
                + "). If this form has permission checkboxes, make sure at least one is selected before submitting.");
        return "error";
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingServletRequestParameter(MissingServletRequestParameterException e, Model model) {
        logger.warn("Form submission rejected: {}", e.getMessage());
        addCommonAttributes(model);
        model.addAttribute("errorMsg", "The form is missing a required field: " + e.getParameterName() + ".");
        return "error";
    }

    // Catches anything a controller method itself doesn't try/catch (this class's other handlers
    // above take precedence for their specific exception types). Same showUnhandledErrors
    // convention IndexController's own per-endpoint catch-all blocks use.
    @ExceptionHandler(Exception.class)
    public String handleUnhandled(Exception e, Model model) {
        logger.error("Unhandled exception reached GlobalExceptionHandler", e);
        addCommonAttributes(model);
        model.addAttribute("errorMsg", showUnhandledErrors ? e.toString() : "Internal Server Error");
        return "error";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("obpBaseUrl", obpBaseUrl);
        model.addAttribute("showBankLogo", showBankLogo);
        model.addAttribute("bankLogoUrl", bankLogoUrl);
    }
}
