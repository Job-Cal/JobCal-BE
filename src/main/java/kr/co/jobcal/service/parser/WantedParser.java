package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;

public class WantedParser extends BaseParser {

    public WantedParser(String html) {
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
            String responsibilities = extractSection(List.of("주요업무", "업무내용", "담당업무", "Responsibilities", "Role"));
            String requirements = extractSection(List.of("자격요건", "필수요건", "Requirements", "Qualifications"));
            String preferences = extractSection(List.of("우대사항", "Preferences", "Preferred"));

            if (description == null || description.length() < 50) {
                StringBuilder merged = new StringBuilder();
                if (responsibilities != null) {
                    merged.append("주요업무: ").append(responsibilities);
                }
                if (requirements != null) {
                    if (merged.length() > 0) {
                        merged.append(" | ");
                    }
                    merged.append("자격요건: ").append(requirements);
                }
                if (preferences != null) {
                    if (merged.length() > 0) {
                        merged.append(" | ");
                    }
                    merged.append("우대사항: ").append(preferences);
                }
                if (merged.length() > 0) {
                    description = merged.toString();
                }
            }

            result.setCompanyName(companyName != null ? companyName : "Unknown Company");
            result.setJobTitle(jobTitle != null ? jobTitle : "Unknown Position");
            result.setDeadline(deadline);
            result.setDescription(description);
            result.setLocation(trimToMax(location, 1000));
            Map<String, Object> parsedData = new HashMap<>();
            parsedData.put("source", "wanted");
            if (responsibilities != null) {
                parsedData.put("responsibilities", responsibilities);
            }
            if (requirements != null) {
                parsedData.put("requirements", requirements);
            }
            if (preferences != null) {
                parsedData.put("preferences", preferences);
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
        Element companyLink = document.selectFirst("a[class*=\"JobHeader_JobHeader__Tools__Company__Link\"]");
        if (companyLink != null) {
            String text = cleanText(companyLink.text());
            if (!text.isBlank()) {
                return text;
            }
            if (companyLink.hasAttr("data-company-name")) {
                String dataName = cleanText(companyLink.attr("data-company-name"));
                if (!dataName.isBlank()) {
                    return dataName;
                }
            }
        }

        List<String> selectors = List.of(
            "h2[class*=company]",
            ".company-name",
            "[data-testid=company-name]",
            "h2"
        );

        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String text = cleanText(element.text());
                if (!text.isBlank() && text.length() < 100) {
                    return text;
                }
            }
        }
        return null;
    }

    private String extractJobTitle() {
        List<String> selectors = List.of(
            "h1[class*=title]",
            "h1[class*=position]",
            "[data-testid=job-title]",
            "h1"
        );

        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String text = cleanText(element.text());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private LocalDate extractDeadline() {
        List<String> keywords = List.of("마감", "deadline", "지원마감", "채용마감");
        for (String keyword : keywords) {
            for (Element element : document.getAllElements()) {
                String text = element.text();
                if (text != null && text.toLowerCase().contains(keyword)) {
                    LocalDate date = extractDate(cleanText(text));
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
            "[class*=description]",
            "[class*=content]",
            ".job-description"
        );

        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String text = cleanText(element.text());
                if (text.length() > 50) {
                    return text.length() > 1000 ? text.substring(0, 1000) : text;
                }
            }
        }
        return null;
    }

    private String extractSection(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }

        for (Element heading : document.select("h1, h2, h3, h4, h5, strong, dt, th")) {
            String headingText = cleanText(heading.text());
            if (containsKeyword(headingText, keywords)) {
                String collected = collectSectionText(heading);
                if (collected != null && !collected.isBlank()) {
                    return collected;
                }
            }
        }

        for (Element element : document.getAllElements()) {
            String text = cleanText(element.text());
            if (containsKeyword(text, keywords)) {
                String parentText = element.parent() != null ? cleanText(element.parent().text()) : text;
                if (parentText.length() > 30 && parentText.length() < 1000) {
                    return parentText;
                }
            }
        }

        return null;
    }

    private boolean containsKeyword(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSectionHeading(Element element) {
        String tag = element.tagName().toLowerCase();
        return tag.matches("h1|h2|h3|h4|h5|strong|dt|th");
    }

    private String collectSectionText(Element heading) {
        StringBuilder builder = new StringBuilder();
        Element next = heading.nextElementSibling();
        int guard = 0;
        while (next != null && guard < 6) {
            if (isSectionHeading(next)) {
                break;
            }
            String text = cleanText(next.text());
            if (!text.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(text);
            }
            next = next.nextElementSibling();
            guard++;
        }
        String result = builder.toString().trim();
        return result.isBlank() ? null : result;
    }

    private String extractLocation() {
        Element companyInfo = document.selectFirst("span[class*=\"JobHeader_JobHeader__Tools__Company__Info\"]");
        if (companyInfo != null) {
            String text = cleanText(companyInfo.text());
            if (!text.isBlank()) {
                return text;
            }
        }

        Element locationTestId = document.selectFirst("[data-testid*=location], [data-testid*=company-location]");
        if (locationTestId != null) {
            String text = cleanText(locationTestId.text());
            if (!text.isBlank()) {
                return text;
            }
        }

        List<String> keywords = List.of("위치", "location", "근무지");
        for (String keyword : keywords) {
            for (Element element : document.getAllElements()) {
                String text = element.text();
                if (text != null && text.toLowerCase().contains(keyword)) {
                    String cleaned = cleanText(text);
                    if (!cleaned.isBlank()) {
                        return cleaned;
                    }
                }
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
