package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class InthisworkParser extends BaseParser {
    private static final int DESCRIPTION_MAX_LENGTH = 10000;

    public InthisworkParser(String html) {
        super(html);
    }

    @Override
    public ParsedJob parse() {
        ParsedJob result = new ParsedJob();
        try {
            String companyName = extractCompanyName();
            String jobTitle = extractJobTitle();
            LocalDate deadline = extractDeadline();
            String descriptionRaw = extractDescription();
            String description = buildSectionDescription(descriptionRaw, companyName);
            if (description == null || description.isBlank()) {
                description = descriptionRaw;
            }
            String location = extractLocation();
            String employmentType = extractEmploymentType();
            String applyUrl = extractApplyUrl();

            result.setCompanyName(companyName != null ? companyName : "Unknown Company");
            result.setJobTitle(jobTitle != null ? jobTitle : "Unknown Position");
            result.setDeadline(deadline);
            result.setDescription(description);
            result.setDescriptionRaw(descriptionRaw);
            result.setLocation(trimToMax(location, 1000));

            Map<String, Object> parsedData = new HashMap<>();
            parsedData.put("source", "inthiswork");
            if (employmentType != null) {
                parsedData.put("employmentType", employmentType);
            }
            if (applyUrl != null) {
                parsedData.put("applyUrl", applyUrl);
            }
            result.setParsedData(parsedData);
        } catch (Exception e) {
            result.setCompanyName("Unknown Company");
            result.setJobTitle("Unknown Position");
            result.setParsedData(Map.of("error", e.getMessage()));
        }
        return result;
    }

    private String extractCompanyName() {
        String title = extractTitleCandidate();
        if (title != null && title.contains("｜")) {
            String company = cleanText(title.split("｜", 2)[0]);
            if (!company.isBlank()) {
                return company;
            }
        }

        for (Element heading : document.select("#content .post h5.wp-block-heading, .post h5.wp-block-heading")) {
            String text = cleanText(heading.text());
            if (!text.isBlank() && !text.equalsIgnoreCase("Affiliation") && !text.contains("합류")) {
                return text;
            }
        }

        return null;
    }

    private String extractJobTitle() {
        String title = extractTitleCandidate();
        if (title != null) {
            String cleaned = cleanText(title);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }

        return null;
    }

    private LocalDate extractDeadline() {
        List<String> keywords = List.of("마감", "지원마감", "채용마감", "접수마감", "모집마감", "~", "deadline");

        String title = extractTitleCandidate();
        if (title != null) {
            LocalDate titleDate = extractDate(title);
            if (titleDate != null) {
                return titleDate;
            }
        }

        for (Element element : document.getAllElements()) {
            String text = cleanText(element.text());
            if (text.isBlank()) {
                continue;
            }
            String lower = text.toLowerCase();
            for (String keyword : keywords) {
                if (lower.contains(keyword.toLowerCase())) {
                    LocalDate date = extractDate(text);
                    if (date != null) {
                        return date;
                    }
                }
            }
        }
        return null;
    }

    private String extractDescription() {
        List<String> selectors = List.of(
            "#content .post .fusion-content-tb-2",
            "#content .post .fusion-content-tb-1",
            ".post .fusion-content-tb-2",
            ".post .fusion-content-tb-1"
        );

        String best = null;
        for (String selector : selectors) {
            for (Element element : document.select(selector)) {
                String text = sanitizeDescription(element.wholeText());
                if (text != null && text.length() > 30 && (best == null || text.length() > best.length())) {
                    best = text;
                }
            }
        }
        if (best != null) {
            return trimToMax(best, DESCRIPTION_MAX_LENGTH);
        }

        List<String> fallbackSelectors = List.of("meta[property=og:description]", "meta[name=description]");
        for (String selector : fallbackSelectors) {
            Element meta = document.selectFirst(selector);
            if (meta != null && meta.hasAttr("content")) {
                String text = sanitizeDescription(meta.attr("content"));
                if (text != null && text.length() > 30) {
                    return trimToMax(text, DESCRIPTION_MAX_LENGTH);
                }
            }
        }

        return null;
    }

    private String extractLocation() {
        for (Element element : document.select("#content .post p, #content .post li, .post p, .post li")) {
            String text = cleanText(element.text());
            if (text.isBlank()) {
                continue;
            }

            String lower = text.toLowerCase();
            if (lower.contains("근무지") || lower.contains("근무지역") || lower.contains("위치") || lower.contains("location")) {
                return text;
            }
        }

        return null;
    }

    private String extractEmploymentType() {
        Elements headings = document.select("#content .post .fusion-content-tb-2 h5.wp-block-heading, #content .post .fusion-content-tb-1 h5.wp-block-heading, .post .fusion-content-tb-2 h5.wp-block-heading, .post .fusion-content-tb-1 h5.wp-block-heading");
        for (int i = 0; i < headings.size() - 1; i++) {
            String current = cleanText(headings.get(i).text());
            if (current.equalsIgnoreCase("Affiliation") || current.contains("Affiliation")) {
                String next = cleanText(headings.get(i + 1).text());
                if (!next.isBlank()) {
                    return next;
                }
            }
        }
        return null;
    }

    private String extractApplyUrl() {
        List<String> selectors = List.of(
            "#content .post a.maxbutton[href]",
            ".post a.maxbutton[href]",
            "#content .post a[href*=\"toss.im/career\"]",
            ".post a[href*=\"toss.im/career\"]"
        );

        for (String selector : selectors) {
            Element link = document.selectFirst(selector);
            if (link != null && link.hasAttr("href")) {
                String href = cleanText(link.attr("href"));
                if (!href.isBlank()) {
                    return href;
                }
            }
        }

        return null;
    }

    private String extractTitleCandidate() {
        List<String> selectors = List.of(
            "section.fusion-page-title-bar h1.fusion-title-heading",
            "meta[property=og:title]",
            "title"
        );

        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element == null) {
                continue;
            }

            String text = element.hasAttr("content") ? cleanText(element.attr("content")) : cleanText(element.text());
            if (text.isBlank()) {
                continue;
            }

            String normalized = text.replace("– IN THIS WORK · 인디스워크", "").trim();
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalizeRawText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00a0', ' ');

        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        int blankRun = 0;

        for (String line : lines) {
            String cleanedLine = line
                .replace('\t', ' ')
                .replace('\f', ' ')
                .stripTrailing();

            if (cleanedLine.isBlank()) {
                blankRun++;
                if (blankRun <= 2 && builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                    builder.append('\n');
                }
                continue;
            }

            blankRun = 0;
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            builder.append(cleanedLine);
        }

        return builder.toString().trim();
    }

    private String sanitizeDescription(String rawText) {
        boolean hadApplyCta = rawText != null && rawText.contains("지원하러 가기");
        String text = normalizeRawText(rawText);
        if (text.isBlank()) {
            return null;
        }

        text = truncateByNoiseMarkers(text);
        text = removeSecondOccurrenceFromAnchors(text);
        text = dedupeConsecutiveLines(text);
        if (hadApplyCta && !text.contains("지원하러 가기")) {
            text = text + "\n지원하러 가기";
        }

        text = text.trim();
        return text.isBlank() ? null : text;
    }

    private String truncateByNoiseMarkers(String text) {
        List<String> markers = List.of(
            "최신 댓글 모음 보러가기",
            "취업토크 추천 아티클",
            "오늘 핫한 공고",
            "Related Posts",
            "0 Comments on",
            "채용공고 공유받는 카톡 채팅방",
            "Unpublish ON",
            "Kakaotalk"
        );

        int cutIndex = -1;
        for (String marker : markers) {
            int idx = text.indexOf(marker);
            if (idx >= 0 && (cutIndex < 0 || idx < cutIndex)) {
                cutIndex = idx;
            }
        }
        if (cutIndex >= 0) {
            return text.substring(0, cutIndex).trim();
        }
        return text;
    }

    private String removeSecondOccurrenceFromAnchors(String text) {
        List<String> anchors = List.of(
            "이런 일을 해요!",
            "이런 분과 함께하고 싶어요!",
            "이런 경험이 있으면 더! 좋아요",
            "포지션 정보",
            "합류 여정",
            "지원 시 유의사항"
        );

        int secondStart = -1;
        for (String anchor : anchors) {
            int first = text.indexOf(anchor);
            if (first < 0) {
                continue;
            }
            int second = text.indexOf(anchor, first + anchor.length());
            if (second > 0 && (secondStart < 0 || second < secondStart)) {
                secondStart = second;
            }
        }
        if (secondStart > 0) {
            return text.substring(0, secondStart).trim();
        }
        return text;
    }

    private String dedupeConsecutiveLines(String text) {
        String[] lines = text.split("\n");
        StringBuilder builder = new StringBuilder();
        String prevNormalized = null;

        for (String line : lines) {
            String normalized = line.strip().replaceAll("\\s+", " ");
            if (prevNormalized != null && !normalized.isBlank() && normalized.equals(prevNormalized)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.stripTrailing());
            if (!normalized.isBlank()) {
                prevNormalized = normalized;
            }
        }

        return builder.toString();
    }

    private String buildSectionDescription(String raw, String companyName) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        List<String> introLines = new ArrayList<>();
        String currentSection = null;

        String[] lines = raw.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            String sectionName = detectSectionHeading(trimmed);
            if (sectionName != null) {
                currentSection = sectionName;
                sections.computeIfAbsent(currentSection, k -> new ArrayList<>());
                continue;
            }

            List<String> items = splitItems(trimmed);
            if (currentSection == null) {
                introLines.addAll(items);
            } else {
                sections.computeIfAbsent(currentSection, k -> new ArrayList<>()).addAll(items);
            }
        }

        if (!introLines.isEmpty()) {
            sections.putIfAbsent("회사소개", new ArrayList<>(introLines));
        } else if (companyName != null && !companyName.isBlank()) {
            sections.putIfAbsent("회사소개", List.of(companyName + " 채용 공고입니다."));
        }

        if (sections.isEmpty()) {
            return raw;
        }

        List<String> order = List.of(
            "회사소개",
            "이런 일을 해요",
            "주요업무",
            "자격요건",
            "우대사항",
            "고용조건",
            "포지션 정보",
            "합류 여정",
            "지원 시 유의사항"
        );

        StringBuilder out = new StringBuilder();
        for (String key : order) {
            List<String> values = sections.get(key);
            if (values == null || values.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append("## **").append(key).append("**");
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                out.append("\n- ").append(value);
            }
        }

        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            String key = entry.getKey();
            if (order.contains(key)) {
                continue;
            }
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append("## **").append(key).append("**");
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                out.append("\n- ").append(value);
            }
        }

        String result = out.toString().trim();
        return result.isBlank() ? raw : result;
    }

    private String detectSectionHeading(String line) {
        String normalized = line
            .replace("!", "")
            .replace(":", "")
            .replace("🙋🏻‍♀️", "")
            .replace("🙆🏻‍♀️", "")
            .replace("🙆🏻‍♂️", "")
            .trim();

        if (normalized.contains("이런 일을 해요")) return "이런 일을 해요";
        if (normalized.contains("주요업무")) return "주요업무";
        if (normalized.contains("자격요건")) return "자격요건";
        if (normalized.contains("우대사항") || normalized.contains("이런 경험이 있으면 더")) return "우대사항";
        if (normalized.contains("고용조건") || normalized.contains("근무조건")) return "고용조건";
        if (normalized.contains("포지션 정보")) return "포지션 정보";
        if (normalized.contains("합류 여정") || normalized.contains("전형")) return "합류 여정";
        if (normalized.contains("지원 시 유의사항")) return "지원 시 유의사항";
        if (normalized.contains("회사소개") || normalized.contains("포지션 상세")) return "회사소개";
        return null;
    }

    private List<String> splitItems(String line) {
        String normalized = line
            .replaceAll("\\s+[•·]\\s+", " • ")
            .replaceAll("^[•·]\\s*", "")
            .trim();

        if (!normalized.contains(" • ")) {
            return List.of(normalized);
        }

        String[] parts = normalized.split("\\s+•\\s+");
        List<String> items = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                items.add(trimmed);
            }
        }
        return items.isEmpty() ? List.of(normalized) : items;
    }
}
