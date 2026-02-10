package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;

public class GenericParser extends BaseParser {

    public GenericParser(String html) {
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
            result.setParsedData(Map.of("source", "generic"));
        } catch (Exception e) {
            result.setCompanyName("Unknown Company");
            result.setJobTitle("Unknown Position");
            result.setParsedData(Map.of("error", e.getMessage()));
        }
        return result;
    }

    private String extractCompanyName() {
        Element meta = document.selectFirst("meta[property=og:site_name]");
        if (meta != null && meta.hasAttr("content")) {
            return cleanText(meta.attr("content"));
        }

        List<String> selectors = List.of(
            "h1",
            "h2",
            "h3",
            "[class*=company]",
            "[id*=company]"
        );

        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String text = cleanText(element.text());
                if (!text.isBlank() && text.length() >= 2 && text.length() <= 50) {
                    return text;
                }
            }
        }
        return null;
    }

    private String extractJobTitle() {
        Element meta = document.selectFirst("meta[property=og:title]");
        if (meta != null && meta.hasAttr("content")) {
            String title = meta.attr("content");
            if (title.contains(" - ")) {
                title = title.split(" - ", 2)[0];
            }
            return cleanText(title);
        }

        Element titleTag = document.selectFirst("title");
        if (titleTag != null) {
            String title = titleTag.text();
            if (title.contains(" - ")) {
                title = title.split(" - ", 2)[0];
            }
            return cleanText(title);
        }

        Element h1 = document.selectFirst("h1");
        if (h1 != null) {
            return cleanText(h1.text());
        }

        return null;
    }

    private LocalDate extractDeadline() {
        List<String> keywords = List.of("마감", "deadline", "지원마감", "채용마감", "접수마감", "모집마감", "채용기간");
        String allText = document.text();

        for (String keyword : keywords) {
            if (allText != null && allText.toLowerCase().contains(keyword)) {
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
        }
        return null;
    }

    private String extractDescription() {
        Element meta = document.selectFirst("meta[property=og:description]");
        if (meta != null && meta.hasAttr("content")) {
            return cleanText(meta.attr("content"));
        }

        Element metaDesc = document.selectFirst("meta[name=description]");
        if (metaDesc != null && metaDesc.hasAttr("content")) {
            return cleanText(metaDesc.attr("content"));
        }

        List<String> selectors = List.of(
            "[class*=description]",
            "[class*=content]",
            "[class*=detail]",
            "main",
            "article"
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
        List<String> keywords = List.of("위치", "location", "근무지", "근무지역", "지역");
        for (String keyword : keywords) {
            for (Element element : document.getAllElements()) {
                String text = element.text();
                if (text != null && text.toLowerCase().contains(keyword)) {
                    String cleaned = cleanText(text);
                    if (!cleaned.isBlank() && cleaned.length() < 100) {
                        return cleaned;
                    }
                }
            }
        }
        return null;
    }
}
