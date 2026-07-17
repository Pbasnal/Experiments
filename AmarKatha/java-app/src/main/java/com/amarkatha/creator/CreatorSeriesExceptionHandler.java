package com.amarkatha.creator;

import com.amarkatha.publishing.SeriesAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = {CreatorSeriesController.class, CreatorController.class})
public class CreatorSeriesExceptionHandler {

    @ExceptionHandler(SeriesAccessException.class)
    public String handleAccess(SeriesAccessException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/creator/series";
    }
}
