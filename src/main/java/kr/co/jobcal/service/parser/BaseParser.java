package kr.co.jobcal.service.parser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class BaseParser {

    protected final Document document;

    protected BaseParser(String html) {
        this.document = Jsoup.parse(html);
    }

    public abstract ParsedJob parse();

    protected LocalDate extractDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        List<Pattern> patterns = List.of(
            Pattern.compile("(\\d{4})[.-](\\d{1,2})[.-](\\d{1,2})"),
            Pattern.compile("(\\d{1,2})[.-](\\d{1,2})[.-](\\d{4})"),
            Pattern.compile("(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    if (matcher.group(1).length() == 4) {
                        int year = Integer.parseInt(matcher.group(1));
                        int month = Integer.parseInt(matcher.group(2));
                        int day = Integer.parseInt(matcher.group(3));
                        return LocalDate.of(year, month, day);
                    } else {
                        int day = Integer.parseInt(matcher.group(1));
                        int month = Integer.parseInt(matcher.group(2));
                        int year = Integer.parseInt(matcher.group(3));
                        return LocalDate.of(year, month, day);
                    }
                } catch (RuntimeException ignored) {
                    continue;
                }
            }
        }

        for (DateTimeFormatter formatter : dateFormatters()) {
            try {
                return LocalDate.parse(text.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                continue;
            }
        }

        return null;
    }

    protected String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private List<DateTimeFormatter> dateFormatters() {
        List<DateTimeFormatter> formatters = new ArrayList<>();
        formatters.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        formatters.add(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        formatters.add(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        formatters.add(DateTimeFormatter.ofPattern("yyyy MM dd", Locale.KOREAN));
        return formatters;
    }
}
