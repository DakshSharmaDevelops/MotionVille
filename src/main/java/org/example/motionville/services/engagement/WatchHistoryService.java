package org.example.motionville.services.engagement;

import org.example.motionville.dto.engagement.WatchHistoryResponse;
import org.example.motionville.dto.engagement.WatchHistoryUpdateRequest;

import java.util.List;

public interface WatchHistoryService {

    WatchHistoryResponse recordProgress(Long videoId, WatchHistoryUpdateRequest request);

    List<WatchHistoryResponse> getHistory(Long userId);

    void removeHistoryItem(Long userId, Long videoId);

    void clearHistory(Long userId);
}
