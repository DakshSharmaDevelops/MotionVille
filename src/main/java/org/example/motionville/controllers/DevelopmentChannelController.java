package org.example.motionville.controllers;

import jakarta.validation.Valid;
import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.DevelopmentChannelRequest;
import org.example.motionville.services.DevelopmentChannelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev/channels")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class DevelopmentChannelController {

    private final DevelopmentChannelService developmentChannelService;

    public DevelopmentChannelController(
            DevelopmentChannelService developmentChannelService) {
        this.developmentChannelService = developmentChannelService;
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(
            @Valid @RequestBody DevelopmentChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(developmentChannelService.createChannel(request));
    }
}
