package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.PlayListCreateRequest;
import org.example.motionville.dto.PlayListResponse;
import org.example.motionville.dto.PlayListUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.playlist.PlayList;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.playlist.PlayListRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayListServiceImplements implements PlayListService {

    private final PlayListRepository playListRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlayListResponse> getAllPlayList(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw notFound("User", userId);
        }
        return playListRepository.findByOwner_Id(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlayListResponse getPlayList(Long playListId) {
        return toResponse(findPlayList(playListId));
    }

    @Override
    @Transactional
    public PlayListResponse savePlayList(PlayListCreateRequest request) {
        AppUser owner = appUserRepository.findById(request.getOwnerId())
                .orElseThrow(() -> notFound("User", request.getOwnerId()));

        PlayList playList = new PlayList();
        playList.setOwner(owner);
        playList.setTitle(request.getTitle().trim());
        playList.setDescription(request.getDescription());
        if (request.getVisibility() != null) {
            playList.setVisibility(request.getVisibility());
        }
        return toResponse(playListRepository.save(playList));
    }

    @Override
    @Transactional
    public PlayListResponse updatePlayList(Long playListId, PlayListUpdateRequest request) {
        PlayList playList = findPlayList(playListId);
        playList.setTitle(request.getTitle().trim());
        playList.setDescription(request.getDescription());
        if (request.getVisibility() != null) {
            playList.setVisibility(request.getVisibility());
        }
        return toResponse(playListRepository.save(playList));
    }

    @Override
    @Transactional
    public void deletePlayList(Long playListId) {

        playListRepository.delete(findPlayList(playListId));
    }

    private PlayList findPlayList(Long playListId) {
        return playListRepository.findById(playListId)
                .orElseThrow(() -> notFound("Playlist", playListId));
    }

    private PlayListResponse toResponse(PlayList playList) {
        PlayListResponse response = new PlayListResponse();
        response.setId(playList.getId());
        response.setOwnerId(playList.getOwner().getId());
        response.setTitle(playList.getTitle());
        response.setDescription(playList.getDescription());
        response.setVisibility(playList.getVisibility());
        response.setCreatedAt(playList.getCreatedAt());
        response.setUpdatedAt(playList.getUpdatedAt());
        response.setVideoIds(playList.getVideos() == null ? List.of()
                : playList.getVideos().stream()
                    .map(item -> item.getVideo().getVideoId())
                    .toList());
        return response;
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }
}
