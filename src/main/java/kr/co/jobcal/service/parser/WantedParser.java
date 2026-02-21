package kr.co.jobcal.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;

public class WantedParser extends BaseParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public WantedParser(String html) {
        super(html);
    }

    @Override
    public ParsedJob parse() {
        ParsedJob result = new ParsedJob();
        try {
            JsonNode initialData = extractWantedInitialDataNode();

            String companyName = extractCompanyName(initialData);
            String jobTitle = extractJobTitle(initialData);
            LocalDate deadline = extractDeadline(initialData);
            String description = extractDescription(initialData);
            String location = extractLocation(initialData);
            String responsibilities = extractResponsibilities(initialData);
            String requirements = extractRequirements(initialData);
            String preferences = extractPreferences(initialData);
            String employmentType = extractEmploymentType(initialData);
            String hireRounds = extractHireRounds(initialData);
            String confirmTime = textAt(initialData, "confirm_time");

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
            if (employmentType != null) {
                parsedData.put("employmentType", employmentType);
            }
            if (hireRounds != null) {
                parsedData.put("hireRounds", hireRounds);
            }
            if (confirmTime != null) {
                parsedData.put("confirmTime", confirmTime);
            }
            result.setParsedData(parsedData);
        } catch (Exception e) {
            result.setCompanyName("Unknown Company");
            result.setJobTitle("Unknown Position");
            result.setParsedData(Map.of("error", e.getMessage()));
        }
        return result;
    }

    private String extractCompanyName(JsonNode initialData) {
        String companyName = textAt(initialData, "company", "company_name");
        if (companyName != null) {
            return companyName;
        }

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

    private String extractJobTitle(JsonNode initialData) {
        String position = textAt(initialData, "position");
        if (position != null) {
            return position;
        }

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

    private LocalDate extractDeadline(JsonNode initialData) {
        LocalDate fromDueTime = parseIsoDate(textAt(initialData, "due_time"));
        if (fromDueTime != null) {
            return fromDueTime;
        }

        LocalDate fromLdJson = parseIsoDate(extractFromJobPostingLdJson("validThrough"));
        if (fromLdJson != null) {
            return fromLdJson;
        }

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

    private String extractDescription(JsonNode initialData) {
        String fromInitialData = mergeDescriptionFromInitialData(initialData);
        if (fromInitialData != null) {
            return trimToMax(fromInitialData, 1000);
        }

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

    private String extractLocation(JsonNode initialData) {
        String fullLocation = textAt(initialData, "address", "full_location");
        if (fullLocation != null) {
            return fullLocation;
        }

        String locality = textAt(initialData, "address", "location");
        String district = textAt(initialData, "address", "district");
        if (locality != null || district != null) {
            if (locality == null) {
                return district;
            }
            if (district == null) {
                return locality;
            }
            return locality + " " + district;
        }

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

    private String extractResponsibilities(JsonNode initialData) {
        String mainTasks = textAt(initialData, "main_tasks");
        if (mainTasks != null) {
            return mainTasks;
        }
        return extractSection(List.of("주요업무", "업무내용", "담당업무", "Responsibilities", "Role"));
    }

    private String extractRequirements(JsonNode initialData) {
        String requirements = textAt(initialData, "requirements");
        if (requirements != null) {
            return requirements;
        }
        return extractSection(List.of("자격요건", "필수요건", "Requirements", "Qualifications"));
    }

    private String extractPreferences(JsonNode initialData) {
        String preferredPoints = textAt(initialData, "preferred_points");
        if (preferredPoints != null) {
            return preferredPoints;
        }
        return extractSection(List.of("우대사항", "Preferences", "Preferred"));
    }

    private String extractEmploymentType(JsonNode initialData) {
        String employmentType = textAt(initialData, "employment_type");
        if (employmentType != null) {
            return employmentType;
        }
        return extractFromJobPostingLdJson("employmentType");
    }

    private String extractHireRounds(JsonNode initialData) {
        return textAt(initialData, "hire_rounds");
    }

    private String mergeDescriptionFromInitialData(JsonNode initialData) {
        List<String> keys = List.of("main_tasks", "requirements", "preferred_points", "benefits", "intro");
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            String value = textAt(initialData, key);
            if (value == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(value);
        }
        String merged = cleanText(builder.toString());
        return merged.isBlank() ? null : merged;
    }

    private JsonNode extractWantedInitialDataNode() {
        Element nextData = document.selectFirst("script#__NEXT_DATA__");
        if (nextData == null) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(nextData.html());
            JsonNode initialData = root.path("props").path("pageProps").path("initialData");
            return initialData.isMissingNode() || initialData.isNull() ? null : initialData;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractFromJobPostingLdJson(String fieldName) {
        for (Element script : document.select("script[type='application/ld+json']")) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(script.html());
                if (!"JobPosting".equals(node.path("@type").asText())) {
                    continue;
                }
                JsonNode value = node.get(fieldName);
                if (value != null && !value.isNull()) {
                    return cleanText(value.asText());
                }
            } catch (Exception ignored) {
                continue;
            }
        }
        return null;
    }

    private String textAt(JsonNode node, String... path) {
        if (node == null) {
            return null;
        }
        JsonNode current = node;
        for (String key : path) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            current = current.path(key);
        }
        if (current == null || current.isMissingNode() || current.isNull()) {
            return null;
        }
        String value = cleanText(current.asText());
        return value.isBlank() ? null : value;
    }

    private LocalDate parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
        } catch (RuntimeException ignored) {
        }
        return extractDate(value);
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
