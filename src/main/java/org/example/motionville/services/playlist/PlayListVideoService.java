package org.example.motionville.services.playlist;

import org.example.motionville.dto.playlist.PlayListVideoResponse;

import java.util.List;

public interface PlayListVideoService {
    PlayListVideoResponse addVideo(Long playListId, Long videoId);

    void removeVideo(Long playListId, Long videoId);

    List<PlayListVideoResponse> listVideos(Long playListId);

    List<PlayListVideoResponse> reorderVideos(Long playListId, List<Long> videoIds);
}
