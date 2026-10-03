package org.example.motionville.services.playlist;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.playlist.PlayListVideoResponse;
import org.example.motionville.entity.playlist.PlayList;
import org.example.motionville.entity.playlist.PlayListVideo;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.playlist.PlayListRepository;
import org.example.motionville.repo.playlist.PlayListVideoRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayListVideoServiceImplements implements PlayListVideoService {

    private final PlayListVideoRepository playListVideoRepository;
    private final PlayListRepository playListRepository;
    private final VideoRepository videoRepository;

    @Override
    public PlayListVideoResponse addVideo(Long playListId, Long videoId) {

        PlayList playList = findPlayList(playListId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> notFound("Video", videoId));

        if (playListVideoRepository.findByPlayList_IdAndVideo_VideoId(playListId, videoId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Video is already in this playlist");
        }

        List<PlayListVideo> current = playListVideoRepository
                .findByPlayList_IdOrderByPositionAsc(playListId);

        int nextPosition = current.stream()
                .map(PlayListVideo::getPosition)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;

        PlayListVideo link = PlayListVideo.builder()
                .playList(playList)
                .video(video)
                .position(nextPosition)
                .addedAt(Instant.now())
                .build();
        return toResponse(playListVideoRepository.save(link));
    }

    @Override
    public void removeVideo(Long playListId, Long videoId) {
        findPlayList(playListId);
        PlayListVideo link = playListVideoRepository
                .findByPlayList_IdAndVideo_VideoId(playListId, videoId)
                .orElseThrow(() -> notFound("Playlist video", videoId));
        playListVideoRepository.delete(link);
        playListVideoRepository.flush();

        List<PlayListVideo> remaining = playListVideoRepository
                .findByPlayList_IdOrderByPositionAsc(playListId);
        normalizePositions(remaining);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayListVideoResponse> listVideos(Long playListId) {
        findPlayList(playListId);
        return playListVideoRepository.findByPlayList_IdOrderByPositionAsc(playListId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PlayListVideoResponse> reorderVideos(Long playListId, List<Long> videoIds) {
        findPlayList(playListId);
        if (videoIds == null || new HashSet<>(videoIds).size() != videoIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "videoIds must contain each playlist video exactly once");
        }

        List<PlayListVideo> links = playListVideoRepository
                .findByPlayList_IdOrderByPositionAsc(playListId);
        Map<Long, PlayListVideo> linksByVideoId = new HashMap<>();
        for (PlayListVideo link : links) {
            linksByVideoId.put(link.getVideo().getVideoId(), link);
        }
        if (linksByVideoId.size() != videoIds.size()
                || !linksByVideoId.keySet().equals(new HashSet<>(videoIds))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "videoIds must match the videos currently in this playlist");
        }

        moveToTemporaryPositions(links);
        for (int position = 0; position < videoIds.size(); position++) {
            linksByVideoId.get(videoIds.get(position)).setPosition(position);
        }
        playListVideoRepository.saveAllAndFlush(links);
        return playListVideoRepository.findByPlayList_IdOrderByPositionAsc(playListId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void normalizePositions(List<PlayListVideo> links) {
        if (links.isEmpty()) return;
        moveToTemporaryPositions(links);
        for (int position = 0; position < links.size(); position++) {
            links.get(position).setPosition(position);
        }
        playListVideoRepository.saveAllAndFlush(links);
    }

    private void moveToTemporaryPositions(List<PlayListVideo> links) {
        if (links.isEmpty()) return;
        int offset = links.stream()
                .map(PlayListVideo::getPosition)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + links.size() + 1;
        for (PlayListVideo link : links) {
            link.setPosition(link.getPosition() + offset);
        }
        playListVideoRepository.saveAllAndFlush(links);
    }

    private PlayList findPlayList(Long playListId) {
        return playListRepository.findById(playListId)
                .orElseThrow(() -> notFound("Playlist", playListId));
    }

    private PlayListVideoResponse toResponse(PlayListVideo link) {
        Video video = link.getVideo();
        return new PlayListVideoResponse(
                video.getVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                video.getVisibility(),
                video.getProcessingStatus(),
                link.getPosition(),
                link.getAddedAt());
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }
}
