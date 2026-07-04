package com.cantor.journal.aireview;

import com.cantor.journal.paper.FileStorageService;
import com.cantor.journal.review.Recommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gemini API를 호출해 AI 피어리뷰를 생성한다. 생성은 수 분이 걸릴 수 있으므로
 * 비동기로 실행하고 결과를 AiReview 엔티티에 반영한다.
 */
@Component
@Slf4j
public class AiReviewGenerator {

    /** Gemini inline 데이터 제한(요청 20MB)을 고려한 PDF 크기 상한 */
    private static final long MAX_PDF_BYTES = 14L * 1024 * 1024;

    private static final String SYSTEM_PROMPT = """
            당신은 다학제 학술지 'Cantor Journal'의 숙련된 심사위원입니다.
            첨부된 논문 원고(PDF)를 읽고 공정하고 건설적인 피어리뷰를 작성하세요.

            평가 관점: 독창성과 기여도, 방법론과 논증의 타당성, 선행 연구 인용의 적절성,
            글의 명료성과 구성, 학술지 게재 적합성.

            결과는 요청된 JSON 스키마에 맞춰 한국어로 작성하세요.
            - summary: 논문 내용과 기여를 3~5문장으로 요약
            - strengths / weaknesses: 구체적인 근거를 든 항목별 목록
            - commentsToAuthor: 저자가 원고를 개선할 수 있도록 돕는 구체적이고 정중한 조언
            - recommendation: ACCEPT(게재 승인), MINOR_REVISION(소폭 수정), MAJOR_REVISION(대폭 수정), REJECT(게재 불가) 중 하나
            - score: 종합 평점 1(매우 미흡)~5(매우 우수)
            """;

    private final AiReviewRepository repository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String modelId;

    public AiReviewGenerator(AiReviewRepository repository,
                             FileStorageService fileStorageService,
                             ObjectMapper objectMapper,
                             @Value("${app.ai-review.api-key}") String apiKey,
                             @Value("${app.ai-review.model}") String modelId) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelId = modelId;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofMinutes(5));
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String modelId() {
        return modelId;
    }

    /** 논문 스냅샷: 비동기 스레드에서 LAZY 연관관계를 건드리지 않도록 필요한 값만 전달한다. */
    public record PaperSnapshot(String title, String authorsText, String abstractText,
                                String category, String articleType, String storedPath) {
    }

    @Async
    public void generate(Long aiReviewId, PaperSnapshot paper) {
        try {
            byte[] pdf = Files.readAllBytes(fileStorageService.pathFor(paper.storedPath()));
            if (pdf.length > MAX_PDF_BYTES) {
                fail(aiReviewId, "PDF가 너무 큽니다(최대 14MB). 파일을 줄여 다시 시도하세요.");
                return;
            }

            JsonNode response = callGemini(pdf, paper);
            applyResult(aiReviewId, response);
        } catch (RestClientResponseException e) {
            log.warn("Gemini API 호출 실패 (aiReviewId={}): {} {}", aiReviewId, e.getStatusCode(), e.getResponseBodyAsString());
            fail(aiReviewId, "AI 모델 호출에 실패했습니다 (HTTP " + e.getStatusCode().value() + ").");
        } catch (Exception e) {
            log.warn("AI 리뷰 생성 실패 (aiReviewId={})", aiReviewId, e);
            fail(aiReviewId, "AI 리뷰 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private JsonNode callGemini(byte[] pdf, PaperSnapshot paper) {
        String userPrompt = """
                다음 논문을 심사해 주세요.

                제목: %s
                저자: %s
                분야: %s / 유형: %s
                초록: %s
                """.formatted(
                nullSafe(paper.title()), nullSafe(paper.authorsText()),
                nullSafe(paper.category()), nullSafe(paper.articleType()),
                nullSafe(paper.abstractText()));

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(
                                Map.of("inline_data", Map.of(
                                        "mime_type", "application/pdf",
                                        "data", Base64.getEncoder().encodeToString(pdf))),
                                Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema()));

        return restClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelId)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private Map<String, Object> responseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of("type", "STRING"),
                        "strengths", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "weaknesses", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "commentsToAuthor", Map.of("type", "STRING"),
                        "recommendation", Map.of("type", "STRING",
                                "enum", List.of("ACCEPT", "MINOR_REVISION", "MAJOR_REVISION", "REJECT")),
                        "score", Map.of("type", "INTEGER")),
                "required", List.of("summary", "strengths", "weaknesses",
                        "commentsToAuthor", "recommendation", "score"));
    }

    private void applyResult(Long aiReviewId, JsonNode response) throws Exception {
        String text = response.path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText(null);
        if (text == null || text.isBlank()) {
            fail(aiReviewId, "AI 모델이 응답을 생성하지 못했습니다. 잠시 후 다시 시도하세요.");
            return;
        }

        JsonNode result = objectMapper.readTree(text);
        AiReview review = repository.findById(aiReviewId).orElse(null);
        if (review == null) {
            return;
        }
        review.setSummary(result.path("summary").asText(null));
        review.setStrengths(joinItems(result.path("strengths")));
        review.setWeaknesses(joinItems(result.path("weaknesses")));
        review.setCommentsToAuthor(result.path("commentsToAuthor").asText(null));
        review.setRecommendation(parseRecommendation(result.path("recommendation").asText("")));
        int score = result.path("score").asInt(0);
        review.setScore(score >= 1 && score <= 5 ? score : null);
        review.setStatus(AiReviewStatus.COMPLETED);
        review.setCompletedAt(Instant.now());
        repository.save(review);
    }

    private void fail(Long aiReviewId, String message) {
        repository.findById(aiReviewId).ifPresent(review -> {
            review.setStatus(AiReviewStatus.FAILED);
            review.setErrorMessage(message.length() > 500 ? message.substring(0, 500) : message);
            review.setCompletedAt(Instant.now());
            repository.save(review);
        });
    }

    private static String joinItems(JsonNode array) {
        if (!array.isArray()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : array) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(item.asText());
        }
        return sb.toString();
    }

    private static Recommendation parseRecommendation(String value) {
        try {
            return Recommendation.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "(제공되지 않음)" : value;
    }
}
