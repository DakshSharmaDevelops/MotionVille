package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoReactionServiceImplements implements VideoReactionService {
    private final VideoReactionService videoReactionService;


}
