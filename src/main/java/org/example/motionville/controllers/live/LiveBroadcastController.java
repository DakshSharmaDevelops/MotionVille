package org.example.motionville.controllers.live;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.motionville.services.live.LiveBroadcastService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live")
public class LiveBroadcastController {
    private final LiveBroadcastService service;
    public LiveBroadcastController(LiveBroadcastService service) { this.service = service; }
    public record CreateRequest(@NotNull @Positive Long channelId, @NotBlank @Size(max=150) String title,
                                @NotBlank String username, @NotBlank String password) {}
    // MediaMTX sends its parsed publish credentials to this server-side endpoint.
    public record MediaAuthorization(String action, String path, String password) {}

    @PostMapping
    public ResponseEntity<LiveBroadcastService.Studio> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                service.create(request.channelId(), request.title(), request.username(), request.password()));
    }
    @GetMapping
    public List<LiveBroadcastService.Broadcast> list() { return service.list(); }
    @GetMapping("/{id}")
    public LiveBroadcastService.Broadcast get(@PathVariable String id) { return service.get(id); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> end(@PathVariable String id, @RequestHeader("X-Live-Token") String token) {
        service.end(id, token);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/authorize")
    public ResponseEntity<Void> authorize(@RequestBody MediaAuthorization request) {
        return ResponseEntity.status(service.authorize(request.action(), request.path(), request.password()) ? 200 : 403).build();
    }
}
