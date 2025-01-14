package com.shivam.ytsummarizer;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VideoController {
    
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarize(@RequestParam String videoUrl, @RequestParam String prompt) {
        try {
            String videoId = videoService.extractVideoId(videoUrl);
            if (videoId == null) {
                throw new IllegalArgumentException("Invalid YouTube URL.");
            }
            String description = videoService.fetchDescription(videoId);
            String summary = videoService.generateSummary(description, prompt);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
}
