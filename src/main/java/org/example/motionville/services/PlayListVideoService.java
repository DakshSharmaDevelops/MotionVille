package org.example.motionville.services;

import org.example.motionville.dto.PlayListVideoResponse;

import java.util.List;

public interface PlayListVideoService {
    PlayListVideoResponse addVideo(Long playListId, Long videoId);

    void removeVideo(Long playListId, Long videoId);

    List<PlayListVideoResponse> listVideos(Long playListId);

    List<PlayListVideoResponse> reorderVideos(Long playListId, List<Long> videoIds);
}
