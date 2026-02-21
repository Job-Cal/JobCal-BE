package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class InthisworkParser extends BaseParser {

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
            String description = extractDescription();
            String location = extractLocation();
            String employmentType = extractEmploymentType();
            String applyUrl = extractApplyUrl();

            result.setCompanyName(companyName != null ? companyName : "Unknown Company");
            result.setJobTitle(jobTitle != null ? jobTitle : "Unknown Position");
            result.setDeadline(deadline);
            result.setDescription(description);
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
                String text = cleanText(element.text());
                if (text.length() > 30 && (best == null || text.length() > best.length())) {
                    best = text;
                }
            }
        }
        if (best != null) {
            return trimToMax(best, 1000);
        }

        List<String> fallbackSelectors = List.of("meta[property=og:description]", "meta[name=description]");
        for (String selector : fallbackSelectors) {
            Element meta = document.selectFirst(selector);
            if (meta != null && meta.hasAttr("content")) {
                String text = cleanText(meta.attr("content"));
                if (text.length() > 30) {
                    return trimToMax(text, 1000);
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
}
