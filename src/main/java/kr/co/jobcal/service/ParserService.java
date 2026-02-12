package kr.co.jobcal.service;

import kr.co.jobcal.dto.JobPostingCreateRequest;
import kr.co.jobcal.service.parser.BaseParser;
import kr.co.jobcal.service.parser.GenericParser;
import kr.co.jobcal.service.parser.JobKoreaParser;
import kr.co.jobcal.service.parser.ParsedJob;
import kr.co.jobcal.service.parser.WantedParser;
import kr.co.jobcal.global.utils.HttpFetcher;
import org.springframework.stereotype.Service;

@Service
public class ParserService {

    public ParserResult parseUrl(String url) {
        try {
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
            request.setDescription(parsedJob.getDescription());
            request.setLocation(parsedJob.getLocation());

            return ParserResult.success(request);
        } catch (Exception e) {
            return ParserResult.failure("Parsing error: " + e.getMessage());
        }
    }

    private BaseParser getParser(String url, String html) {
        String lower = url.toLowerCase();
        if (lower.contains("wanted.co.kr") || lower.contains("wanted")) {
            return new WantedParser(html);
        }
        if (lower.contains("jobkorea.co.kr") || lower.contains("jobkorea")) {
            return new JobKoreaParser(html);
        }
        return new GenericParser(html);
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
