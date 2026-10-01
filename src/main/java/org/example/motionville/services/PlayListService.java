package org.example.motionville.services;

import org.example.motionville.dto.PlayListCreateRequest;
import org.example.motionville.dto.PlayListResponse;
import org.example.motionville.dto.PlayListUpdateRequest;

import java.util.List;

public interface PlayListService {

    List<PlayListResponse> getAllPlayList(Long userId);

    PlayListResponse getPlayList(Long playListId);

    PlayListResponse savePlayList(PlayListCreateRequest request);

    PlayListResponse updatePlayList(Long playListId, PlayListUpdateRequest request);

    void deletePlayList(Long playListId);
}
