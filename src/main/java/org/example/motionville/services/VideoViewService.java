package org.example.motionville.services;

import org.example.motionville.dto.VideoViewRequest;
import org.example.motionville.dto.VideoViewResponse;

public interface VideoViewService {
    VideoViewResponse recordView(Long videoId, VideoViewRequest request);

    long getViewCount(Long videoId);
}
