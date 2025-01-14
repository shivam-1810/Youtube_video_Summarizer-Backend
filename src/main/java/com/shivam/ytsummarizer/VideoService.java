package com.shivam.ytsummarizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class VideoService {

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractVideoId(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.isEmpty()) {
            throw new IllegalArgumentException("YouTube URL cannot be null or empty");
        }
        String regex = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/.*[?&]v=|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(youtubeUrl);

        if (matcher.find()) {
            System.out.println(matcher.group(1));
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid YouTube URL");
        }
    }

    public String fetchDescription(String videoId) {
        String youtubeApiUrl = String.format("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=%s&key=%s", videoId, youtubeApiKey);
        try {
            String response = webClient.get().uri(youtubeApiUrl).retrieve().bodyToMono(String.class).block();
            JsonNode snippet = objectMapper.readTree(response).path("items").get(0).path("snippet");
            System.out.println(snippet.path("description").asText());
            return snippet.isMissingNode() ? null : snippet.path("description").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching description", e);
        }
    }

    public String generateSummary(String description, String prompt) {
        String geminiApiUrl = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s",
            geminiApiKey
        );
        String payload = String.format(
            "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}",
            description + "\nPrompt: This is the transcripted text from a youtube video, generate the summary of the video - " + prompt
        );
        try {
            String response = webClient.post()
                .uri(geminiApiUrl)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new RuntimeException("API Error: " + error))))
                .bodyToMono(String.class)
                .block();

            System.out.println("Gemini API Response: " + response);

            JsonNode result = objectMapper.readTree(response);
            JsonNode candidates = result.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("Invalid response format: 'candidates' is missing or empty");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new RuntimeException("Invalid response format: 'parts' is missing or empty");
            }

            String rawText = parts.get(0).path("text").asText();
            return cleanAndFormatSummary(rawText);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching summary", e);
        }
    }

    private String cleanAndFormatSummary(String rawText) {
        String cleanedText = rawText.replace("**", "").replace("\\n", "").trim();
        cleanedText = cleanedText.replaceAll("\\*\\s", "\n• ");
        return cleanedText;
    }
}
