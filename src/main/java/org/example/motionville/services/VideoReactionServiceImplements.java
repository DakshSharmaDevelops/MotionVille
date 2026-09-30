package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.repo.engagement.VideoReactionRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoReactionServiceImplements implements VideoReactionService {
    private final VideoReactionRepository videoReactionRepository;


}
