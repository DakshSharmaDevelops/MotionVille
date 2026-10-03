package org.example.motionville.controllers;

import jakarta.validation.Valid;
import org.example.motionville.dto.ChannelCreateRequest;
import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.ChannelUpdateRequest;
import org.example.motionville.services.ChannelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#request.ownerId, authentication)")
    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(
            @Valid @RequestBody ChannelCreateRequest request) {

        ChannelResponse response =
                channelService.createChannel(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getChannelById(
            @PathVariable Long id) {

        ChannelResponse response =
                channelService.getChannelById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getAllChannels(
            @RequestParam(required = false, defaultValue = "") String search) {

        List<ChannelResponse> responses =
                channelService.getAllChannels(search);

        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isChannelOwner(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody ChannelUpdateRequest request) {

        ChannelResponse response =
                channelService.updateChannel(id, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isChannelOwner(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long id) {

        channelService.deleteChannel(id);

        return ResponseEntity.noContent().build();
    }
}