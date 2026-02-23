package kr.co.jobcal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JobDescriptionFormatter {

    private static final List<String> SECTION_HINTS = List.of(
        "포지션 상세",
        "이런 일을 해요",
        "이런 분과 함께하고 싶어요",
        "이런 경험이 있으면 더",
        "주요업무",
        "자격요건",
        "우대사항",
        "고용조건",
        "복지",
        "포지션 정보",
        "합류 여정",
        "지원 시 유의사항"
    );

    public String toMarkdown(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        String normalized = normalizeForSections(raw);
        String[] lines = normalized.split("\n");
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                cleaned.add(trimmed);
            }
        }

        if (cleaned.isEmpty()) {
            return raw;
        }

        StringBuilder out = new StringBuilder();
        for (String line : cleaned) {
            if (isSectionHeading(line)) {
                if (out.length() > 0) {
                    out.append("\n\n");
                }
                out.append("## **").append(stripTrailingPunctuation(line)).append("**");
                continue;
            }

            if (out.length() > 0) {
                out.append('\n');
            }
            out.append("- ").append(stripBulletPrefix(line));
        }

        return out.toString().trim();
    }

    private boolean isSectionHeading(String line) {
        String normalized = line
            .replace("!", "")
            .replace("?", "")
            .replace(":", "")
            .replace("🙋🏻‍♀️", "")
            .replace("🙆🏻‍♀️", "")
            .replace("🙆🏻‍♂️", "")
            .trim();
        for (String hint : SECTION_HINTS) {
            if (normalized.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private String stripBulletPrefix(String line) {
        return line.replaceFirst("^[\\-•·]\\s*", "").trim();
    }

    private String stripTrailingPunctuation(String line) {
        return line.replaceAll("[!?:\\s]+$", "").trim();
    }

    private String normalizeForSections(String raw) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');

        // "주요업무•", "자격요건•"처럼 붙어 있는 케이스 분리
        text = text.replaceAll("(?<=[가-힣A-Za-z0-9)])\\s*[•·]\\s*", "\n• ");

        // 섹션 헤딩 앞에 줄바꿈 강제 (문장 중간 붙음 방지)
        for (String hint : SECTION_HINTS) {
            String pattern = "(?<!\\n)" + Pattern.quote(hint);
            text = text.replaceAll(pattern, "\n" + hint);
        }

        return text.trim();
    }
}
