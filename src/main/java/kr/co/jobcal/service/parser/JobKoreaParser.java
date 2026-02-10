package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;

public class JobKoreaParser extends BaseParser {

    public JobKoreaParser(String html) {
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

            result.setCompanyName(companyName != null ? companyName : "Unknown Company");
            result.setJobTitle(jobTitle != null ? jobTitle : "Unknown Position");
            result.setDeadline(deadline);
            result.setDescription(description);
            result.setLocation(location);
            result.setParsedData(Map.of("source", "jobkorea"));
        } catch (Exception e) {
            result.setCompanyName("Unknown Company");
            result.setJobTitle("Unknown Position");
            result.setParsedData(Map.of("error", e.getMessage()));
        }
        return result;
    }

    private String extractCompanyName() {
        List<String> selectors = List.of(
            ".company-name",
            "[class*=company]",
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
            ".job-title",
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
        List<String> keywords = List.of("마감", "deadline", "지원마감", "채용마감", "접수마감");
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
            ".job-content"
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

    private String extractLocation() {
        List<String> keywords = List.of("위치", "location", "근무지", "근무지역");
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
}
