package org.example.motionville.services.engagement;

import org.example.motionville.dto.engagement.VideoViewRequest;
import org.example.motionville.dto.engagement.VideoViewResponse;

public interface VideoViewService {
    VideoViewResponse recordView(Long videoId, VideoViewRequest request);

    long getViewCount(Long videoId);
}
