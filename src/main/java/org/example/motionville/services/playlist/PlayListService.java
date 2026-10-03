package org.example.motionville.services.playlist;

import org.example.motionville.dto.playlist.PlayListCreateRequest;
import org.example.motionville.dto.playlist.PlayListResponse;
import org.example.motionville.dto.playlist.PlayListUpdateRequest;

import java.util.List;

public interface PlayListService {

    List<PlayListResponse> getAllPlayList(Long userId);

    PlayListResponse getPlayList(Long playListId);

    PlayListResponse savePlayList(PlayListCreateRequest request);

    PlayListResponse updatePlayList(Long playListId, PlayListUpdateRequest request);

    void deletePlayList(Long playListId);
}
