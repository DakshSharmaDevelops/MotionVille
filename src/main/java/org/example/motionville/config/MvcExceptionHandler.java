package org.example.motionville.config;

import org.example.motionville.account.AccountConflictException;
import org.example.motionville.channel.ChannelConflictException;
import org.example.motionville.channel.ResourceNotFoundException;
import org.example.motionville.engagement.EngagementService;
import org.example.motionville.video.MediaStorageException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class MvcExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(AccessDeniedException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    @ExceptionHandler({AccountConflictException.class, ChannelConflictException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(RuntimeException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    @ExceptionHandler(EngagementService.EngagementException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidEngagement(EngagementService.EngagementException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    @ExceptionHandler(MediaStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String mediaError(MediaStorageException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }
}
