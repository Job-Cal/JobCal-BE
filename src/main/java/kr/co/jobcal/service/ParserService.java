package kr.co.jobcal.service;

import kr.co.jobcal.dto.JobPostingCreateRequest;
import kr.co.jobcal.service.parser.BaseParser;
import kr.co.jobcal.service.parser.InthisworkParser;
import kr.co.jobcal.service.parser.ParsedJob;
import kr.co.jobcal.service.parser.WantedParser;
import kr.co.jobcal.global.utils.HttpFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;

@Service
public class ParserService {
    private static final Logger log = LoggerFactory.getLogger(ParserService.class);

    private static final String UNSUPPORTED_URL_ERROR = "지원하지 않는 주소입니다. 원티드/인디스워크 URL만 지원합니다.";
    private final JobDescriptionFormatter jobDescriptionFormatter;

    public ParserService(JobDescriptionFormatter jobDescriptionFormatter) {
        this.jobDescriptionFormatter = jobDescriptionFormatter;
    }

    public ParserResult parseUrl(String url) {
        try {
            if (!isWantedUrl(url)) {
                return ParserResult.failure(UNSUPPORTED_URL_ERROR);
            }

            String html = HttpFetcher.fetchUrl(url);
            if (html == null || html.isBlank()) {
                return ParserResult.failure("Failed to fetch URL");
            }

            BaseParser parser = getParser(url, html);
            ParsedJob parsedJob = parser.parse();

            JobPostingCreateRequest request = new JobPostingCreateRequest();
            request.setCompanyName(parsedJob.getCompanyName() != null ? parsedJob.getCompanyName() : "Unknown Company");
            request.setJobTitle(parsedJob.getJobTitle() != null ? parsedJob.getJobTitle() : "Unknown Position");
            request.setDeadline(parsedJob.getDeadline());
            request.setOriginalUrl(url);
            request.setParsedData(parsedJob.getParsedData());
            String rawDescription = parsedJob.getDescriptionRaw() != null
                ? parsedJob.getDescriptionRaw()
                : parsedJob.getDescription();
            request.setDescriptionRaw(rawDescription);
            String formattedDescription = parsedJob.getDescription();
            if (formattedDescription == null || formattedDescription.isBlank()) {
                formattedDescription = jobDescriptionFormatter.toMarkdown(rawDescription);
            }
            request.setDescription(formattedDescription);
            request.setLocation(parsedJob.getLocation());

            String host = extractNormalizedHost(url);
            if (isInthisworkHost(host)) {
                log.info(
                    "[Inthiswork Parse] url={}, companyName={}, jobTitle={}, deadline={}, location={}, parsedData={}",
                    url,
                    request.getCompanyName(),
                    request.getJobTitle(),
                    request.getDeadline(),
                    request.getLocation(),
                    request.getParsedData()
                );
            }

            return ParserResult.success(request);
        } catch (Exception e) {
            return ParserResult.failure("Parsing error: " + e.getMessage());
        }
    }

    private BaseParser getParser(String url, String html) {
        String host = extractNormalizedHost(url);
        if (isInthisworkHost(host)) {
            return new InthisworkParser(html);
        }

        return new WantedParser(html);
    }

    private boolean isWantedUrl(String url) {
        String host = extractNormalizedHost(url);
        if (host == null) {
            return false;
        }

        return host.equals("wanted.co.kr")
            || host.endsWith(".wanted.co.kr")
            || isInthisworkHost(host);
    }

    private boolean isInthisworkHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        return host.equals("inthiswork.com") || host.endsWith(".inthiswork.com");
    }

    private String extractNormalizedHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }

            return host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static class ParserResult {
        private final boolean success;
        private final JobPostingCreateRequest data;
        private final String error;

        private ParserResult(boolean success, JobPostingCreateRequest data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }

        public static ParserResult success(JobPostingCreateRequest data) {
            return new ParserResult(true, data, null);
        }

        public static ParserResult failure(String error) {
            return new ParserResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public JobPostingCreateRequest getData() {
            return data;
        }

        public String getError() {
            return error;
        }
    }
}
