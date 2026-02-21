package kr.co.jobcal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiDescriptionFormatter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final String openaiApiKey;
    private final String openaiModel;
    private final int maxOutputTokens;
    private final double temperature;

    public AiDescriptionFormatter(
        @Value("${ai.openai.api-key:}") String openaiApiKey,
        @Value("${ai.openai.model:gpt-4.1-mini}") String openaiModel,
        @Value("${ai.openai.max-output-tokens:1400}") int maxOutputTokens,
        @Value("${ai.openai.temperature:0.1}") double temperature
    ) {
        this.openaiApiKey = openaiApiKey;
        this.openaiModel = openaiModel;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    public String formatToMarkdown(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return rawDescription;
        }
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return rawDescription;
        }

        try {
            Map<String, Object> payload = Map.of(
                "model", openaiModel,
                "input", buildPrompt(rawDescription),
                "max_output_tokens", maxOutputTokens,
                "temperature", temperature,
                "top_p", 0.9
            );

            String requestBody = OBJECT_MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(OPENAI_RESPONSES_URL))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + openaiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return rawDescription;
            }

            String markdown = extractOutputText(response.body());
            if (markdown == null || markdown.isBlank()) {
                return rawDescription;
            }
            String trimmed = markdown.trim();
            return isContentPreserved(rawDescription, trimmed) ? trimmed : rawDescription;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return rawDescription;
        }
    }

    private String buildPrompt(String description) {
        return """
            아래 채용공고 본문에 "마크다운 형식만" 추가하세요.
            반드시 지킬 규칙:
            1) 원문 문장, 단어, 숫자, 링크, 기호를 절대 수정하지 마세요.
            2) 요약/재작성/치환/문장 추가 금지.
            3) 순서 그대로 유지.
            4) 허용된 작업은 줄바꿈 정리와 목록 앞에 markdown 불릿(- ) 추가뿐입니다.
            5) 결과는 원문과 동일한 내용이어야 하며 마크다운 본문만 출력하세요.

            원문:
            """ + description + "\n";
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode c : content) {
                    JsonNode text = c.path("text");
                    if (text.isTextual() && !text.asText().isBlank()) {
                        return text.asText();
                    }
                }
            }
        }
        return null;
    }

    private boolean isContentPreserved(String original, String formatted) {
        String a = canonicalize(original);
        String b = canonicalize(formatted);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }

        int aLen = a.length();
        int bLen = b.length();
        double ratio = (double) bLen / (double) aLen;
        if (ratio < 0.85 || ratio > 1.20) {
            return false;
        }

        Set<String> aTokens = tokenize(a);
        Set<String> bTokens = tokenize(b);
        if (aTokens.isEmpty() || bTokens.isEmpty()) {
            return false;
        }

        int intersection = 0;
        for (String token : aTokens) {
            if (bTokens.contains(token)) {
                intersection++;
            }
        }

        double recall = (double) intersection / (double) aTokens.size();
        return recall >= 0.92;
    }

    private String canonicalize(String text) {
        return text
            .replaceAll("(?m)^\\s*[-*+]\\s+", "")
            .replaceAll("(?m)^\\s*#+\\s*", "")
            .replaceAll("`+", "")
            .replaceAll("\\*\\*", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private Set<String> tokenize(String text) {
        String[] parts = text.toLowerCase().split("[^\\p{L}\\p{N}:/._-]+");
        Set<String> tokens = new HashSet<>();
        Arrays.stream(parts)
            .filter(p -> p != null && !p.isBlank() && p.length() >= 2)
            .forEach(tokens::add);
        return tokens;
    }
}
