package org.example.motionville.video.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.HttpRange;
import org.example.motionville.account.MotionVilleUserDetails;
import org.example.motionville.channel.ResourceNotFoundException;
import org.example.motionville.video.Video;
import org.example.motionville.video.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class MediaController {
    private final VideoService videos;

    public MediaController(VideoService videos) {
        this.videos = videos;
    }

    @GetMapping("/media/{storageKey:.+}")
    public ResponseEntity<StreamingResponseBody> video(
            @PathVariable String storageKey,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {
        Video video = videos.findByStorageKey(storageKey)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found."));
        Resource resource = videos.loadMedia(video, user == null ? null : user.getId());
        long length = resource.contentLength();
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(video.getContentType()));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storageKey + "\"");
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (rangeHeader == null) {
            headers.setContentLength(length);
            StreamingResponseBody body = output -> {
                try (InputStream input = resource.getInputStream()) {
                    input.transferTo(output);
                }
            };
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        }
        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.isEmpty()) {
                return rangeNotSatisfiable(length, headers);
            }
            long start = ranges.get(0).getRangeStart(length);
            long end = ranges.get(0).getRangeEnd(length);
            if (start >= length || end < start) {
                return rangeNotSatisfiable(length, headers);
            }
            long count = end - start + 1;
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length);
            headers.setContentLength(count);
            StreamingResponseBody body = output -> {
                try (InputStream input = resource.getInputStream()) {
                    input.skipNBytes(start);
                    byte[] buffer = new byte[8192];
                    long remaining = count;
                    while (remaining > 0) {
                        int bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (bytesRead < 0) {
                            break;
                        }
                        output.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                }
            };
            return new ResponseEntity<>(body, headers, HttpStatus.PARTIAL_CONTENT);
        } catch (IllegalArgumentException exception) {
            return rangeNotSatisfiable(length, headers);
        }
    }

    private ResponseEntity<StreamingResponseBody> rangeNotSatisfiable(
            long length, org.springframework.http.HttpHeaders headers) {
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
        return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }
}
