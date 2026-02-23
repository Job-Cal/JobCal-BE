package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.co.jobcal.global.utils.HttpFetcher;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ZighangParser extends BaseParser {
    private static final int DESCRIPTION_MAX_LENGTH = 10000;

    public ZighangParser(String html) {
        super(html);
    }

    @Override
    public ParsedJob parse() {
        ParsedJob result = new ParsedJob();
        try {
            String fullText = normalizeText(document.wholeText());
            String jobTitle = extractJobTitle();
            String companyName = extractCompanyName(fullText, jobTitle);
            LocalDate deadline = extractDeadline(fullText);
            String experience = extractField(fullText, "경력");
            String employmentType = extractField(fullText, "채용 유형");
            String education = extractField(fullText, "학력");
            String locationLabel = extractField(fullText, "지역");
            String deadlineLabel = extractField(fullText, "마감일");
            String source = normalizeSource(extractField(fullText, "출처"));
            String sourceUrl = extractSourceUrl();
            String proseMirrorRaw = extractProseMirrorDescription();
            String sourceDescription = extractDescriptionFromSource(sourceUrl);
            String descriptionRaw = extractDescription(
                proseMirrorRaw,
                sourceDescription,
                fullText,
                experience,
                employmentType,
                education,
                locationLabel,
                deadlineLabel,
                source
            );
            String description = descriptionRaw;
            String detectedLocation = extractLocation();

            result.setCompanyName(companyName != null ? companyName : "Unknown Company");
            result.setJobTitle(jobTitle != null ? jobTitle : "Unknown Position");
            result.setDeadline(deadline);
            result.setDescription(description);
            result.setDescriptionRaw(descriptionRaw);
            result.setLocation(locationLabel != null ? locationLabel : detectedLocation);

            Map<String, Object> parsedData = new HashMap<>();
            parsedData.put("source", "zighang");
            if (experience != null) {
                parsedData.put("experience", experience);
            }
            if (employmentType != null) {
                parsedData.put("employmentType", employmentType);
            }
            if (education != null) {
                parsedData.put("education", education);
            }
            if (locationLabel != null) {
                parsedData.put("location", locationLabel);
            } else if (detectedLocation != null) {
                parsedData.put("location", detectedLocation);
            }
            if (deadlineLabel != null) {
                parsedData.put("deadlineLabel", deadlineLabel);
            }
            if (source != null) {
                parsedData.put("originSource", source);
            }
            if (sourceUrl != null) {
                parsedData.put("originSourceUrl", sourceUrl);
            }
            result.setParsedData(parsedData);
        } catch (Exception e) {
            result.setCompanyName("Unknown Company");
            result.setJobTitle("Unknown Position");
            result.setParsedData(Map.of("error", e.getMessage()));
        }
        return result;
    }

    private String extractCompanyName(String fullText, String jobTitle) {
        String fromTitle = deriveCompanyFromJobTitle(jobTitle);
        if (fromTitle != null) {
            return fromTitle;
        }

        Pattern p = Pattern.compile("([가-힣A-Za-z0-9()&,.\\-\\s]{2,40})\\|\\s*\\d{4}\\.\\s*\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*게시\\|");
        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String value = cleanText(m.group(1));
            if (!value.isBlank()) {
                return value;
            }
        }

        Element heading = document.selectFirst("h3, [class*=company]");
        if (heading != null) {
            String value = cleanText(heading.text());
            if (!value.isBlank() && !value.equals("회사 소개가 없습니다.")) {
                return value;
            }
        }

        return null;
    }

    private String deriveCompanyFromJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.isBlank()) {
            return null;
        }
        Matcher bracket = Pattern.compile("\\[(.+?)\\]").matcher(jobTitle);
        if (bracket.find()) {
            String value = cleanText(bracket.group(1));
            return value.isBlank() ? null : value;
        }
        return null;
    }

    private String extractJobTitle() {
        List<String> selectors = List.of(
            "meta[property=og:title]",
            "h1",
            "[data-testid*=title]",
            "[class*=title]"
        );

        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element == null) {
                continue;
            }

            String text = element.hasAttr("content")
                ? cleanText(element.attr("content"))
                : cleanText(element.text());
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private LocalDate extractDeadline(String fullText) {
        String value = extractField(fullText, "마감일");
        LocalDate date = extractDate(value);
        if (date != null) {
            return date;
        }
        return extractDate(fullText);
    }

    private String extractDescription(
        String proseMirrorRaw,
        String sourceDescription,
        String fullText,
        String experience,
        String employmentType,
        String education,
        String location,
        String deadlineLabel,
        String source
    ) {
        if (proseMirrorRaw != null && !proseMirrorRaw.isBlank()) {
            return trimToMax(proseMirrorRaw, DESCRIPTION_MAX_LENGTH);
        }
        if (sourceDescription != null && !sourceDescription.isBlank()) {
            return trimToMax(sourceDescription, DESCRIPTION_MAX_LENGTH);
        }

        List<String> lines = new ArrayList<>();
        if (experience != null) lines.add("- 경력: " + experience);
        if (employmentType != null) lines.add("- 채용 유형: " + employmentType);
        if (education != null) lines.add("- 학력: " + education);
        if (location != null) lines.add("- 지역: " + location);
        if (deadlineLabel != null) lines.add("- 마감일: " + deadlineLabel);
        if (source != null) lines.add("- 출처: " + source);

        if (!lines.isEmpty()) {
            String summary = "## **포지션 정보**\n" + String.join("\n", lines);
            return trimToMax(summary, DESCRIPTION_MAX_LENGTH);
        }

        String text = trimToMax(fullText, DESCRIPTION_MAX_LENGTH);
        return text.isBlank() ? null : text;
    }

    private String extractProseMirrorDescription() {
        Elements proseMirrors = document.select(".ProseMirror, .tiptap.ProseMirror");
        if (proseMirrors.isEmpty()) {
            return null;
        }

        StringBuilder out = new StringBuilder();
        for (Element root : proseMirrors) {
            for (Element node : root.select("h1, h2, h3, h4, p, li, blockquote")) {
                String text = cleanText(node.text());
                if (text.isBlank()) {
                    continue;
                }

                if (out.length() > 0) {
                    out.append('\n');
                }

                if ("li".equalsIgnoreCase(node.tagName())) {
                    out.append("- ").append(text);
                } else {
                    out.append(text);
                }
            }
        }

        String result = out.toString().trim();
        return result.isBlank() ? null : result;
    }

    private String extractSourceUrl() {
        Element anchor = document.selectFirst(
            "a[href*='wanted.co.kr'], a[href*='inthiswork.com'], a[href*='jobkorea.co.kr'], a[href*='saramin.co.kr']"
        );
        if (anchor == null || !anchor.hasAttr("href")) {
            return null;
        }
        String href = cleanText(anchor.attr("href"));
        return href.isBlank() ? null : href;
    }

    private String extractDescriptionFromSource(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        try {
            String html = HttpFetcher.fetchUrl(sourceUrl);
            if (html == null || html.isBlank()) {
                return null;
            }

            if (sourceUrl.contains("wanted.co.kr")) {
                ParsedJob parsed = new WantedParser(html).parse();
                return parsed.getDescriptionRaw() != null ? parsed.getDescriptionRaw() : parsed.getDescription();
            }
            if (sourceUrl.contains("inthiswork.com")) {
                ParsedJob parsed = new InthisworkParser(html).parse();
                return parsed.getDescriptionRaw() != null ? parsed.getDescriptionRaw() : parsed.getDescription();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String extractLocation() {
        List<String> keywords = List.of("근무지", "근무지역", "위치", "location", "지역");
        for (Element element : document.getAllElements()) {
            String text = cleanText(element.text());
            if (text.isBlank()) {
                continue;
            }
            String lower = text.toLowerCase();
            for (String keyword : keywords) {
                if (lower.contains(keyword.toLowerCase())) {
                    return text;
                }
            }
        }
        return null;
    }

    private String extractField(String text, String label) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String stopWords = "(경력|채용 유형|학력|지역|마감일|출처|홈페이지|오류제보|이 공고|지원하기|회사 소개가 없습니다\\.|$)";
        Pattern p = Pattern.compile(
            Pattern.quote(label) + "\\s*[:：]?\\s*(.+?)(?=\\s*" + stopWords + ")"
        );
        Matcher m = p.matcher(text);
        if (!m.find()) {
            return null;
        }

        String value = cleanText(m.group(1))
            .replaceAll("\\s*\\|\\s*$", "")
            .trim();
        if (value.isBlank()) {
            return null;
        }
        return value;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00a0', ' ');
        String[] lines = normalized.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String cleaned = line
                .replace('\t', ' ')
                .replaceAll(" {2,}", " ")
                .trim();
            if (cleaned.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(cleaned);
        }
        return builder.toString();
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return source;
        }
        if (source.contains("원티드")) {
            return "원티드";
        }
        if (source.contains("인디스워크")) {
            return "인디스워크";
        }
        if (source.toLowerCase().contains("zighang") || source.contains("직행")) {
            return "직행";
        }
        if (source.contains("잡코리아")) {
            return "잡코리아";
        }
        if (source.contains("사람인")) {
            return "사람인";
        }
        return source;
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
}
