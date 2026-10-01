package org.example.motionville.services;

import org.example.motionville.dto.WatchHistoryResponse;
import org.example.motionville.dto.WatchHistoryUpdateRequest;

import java.util.List;

public interface WatchHistoryService {

    WatchHistoryResponse recordProgress(Long videoId, WatchHistoryUpdateRequest request);

    List<WatchHistoryResponse> getHistory(Long userId);

    void removeHistoryItem(Long userId, Long videoId);

    void clearHistory(Long userId);
}
