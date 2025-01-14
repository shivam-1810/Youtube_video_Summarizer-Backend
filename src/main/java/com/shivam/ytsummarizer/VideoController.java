package com.shivam.ytsummarizer;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class VideoController {
    
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestParam String videoUrl, @RequestParam String prompt) {
            String videoId = videoService.extractVideoId(videoUrl);
            if (videoId == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            String description = videoService.fetchDescription(videoId);
            String summary = videoService.generateSummary(description, prompt);
            return ResponseEntity.ok(Map.of("summary", summary));
    }
}
