package kr.co.jobcal.controller;

import kr.co.jobcal.dto.ApplicationCreateRequest;
import kr.co.jobcal.dto.JobPostingCreateRequest;
import kr.co.jobcal.dto.JobPostingParseRequest;
import kr.co.jobcal.dto.JobPostingParseResponse;
import kr.co.jobcal.dto.JobPostingResponse;
import kr.co.jobcal.entity.ApplicationStatus;
import kr.co.jobcal.entity.JobPosting;
import kr.co.jobcal.service.ApplicationService;
import kr.co.jobcal.service.CurrentUserProvider;
import kr.co.jobcal.service.JobPostingService;
import kr.co.jobcal.service.ParserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final ParserService parserService;
    private final JobPostingService jobPostingService;
    private final ApplicationService applicationService;
    private final CurrentUserProvider currentUserProvider;

    public JobController(
        ParserService parserService,
        JobPostingService jobPostingService,
        ApplicationService applicationService,
        CurrentUserProvider currentUserProvider
    ) {
        this.parserService = parserService;
        this.jobPostingService = jobPostingService;
        this.applicationService = applicationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/parse")
    public JobPostingParseResponse parseJobUrl(@RequestBody JobPostingParseRequest request) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            return new JobPostingParseResponse(false, null, "URL is required");
        }
        ParserService.ParserResult result = parserService.parseUrl(request.getUrl());
        return new JobPostingParseResponse(result.isSuccess(), result.getData(), result.getError());
    }

    @PostMapping("/parse-and-create")
    @ResponseStatus(HttpStatus.OK)
    public JobPostingResponse parseAndCreate(@RequestBody JobPostingParseRequest request) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required");
        }

        ParserService.ParserResult result = parserService.parseUrl(request.getUrl());
        if (!result.isSuccess() || result.getData() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getError() != null ? result.getError() : "Parsing failed");
        }

        JobPosting jobPosting = jobPostingService.createOrUpdate(result.getData());

        ApplicationCreateRequest applicationRequest = new ApplicationCreateRequest();
        applicationRequest.setJobPostingId(jobPosting.getId());
        applicationRequest.setStatus(ApplicationStatus.NOT_APPLIED);
        applicationService.createApplication(currentUserProvider.getCurrentUserId(), applicationRequest);

        return toJobPostingResponse(jobPosting);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public JobPostingResponse createJobPosting(@RequestBody JobPostingCreateRequest request) {
        JobPosting jobPosting = jobPostingService.createOrUpdate(request);

        ApplicationCreateRequest applicationRequest = new ApplicationCreateRequest();
        applicationRequest.setJobPostingId(jobPosting.getId());
        applicationRequest.setStatus(ApplicationStatus.NOT_APPLIED);
        applicationService.createApplication(currentUserProvider.getCurrentUserId(), applicationRequest);

        return toJobPostingResponse(jobPosting);
    }

    @GetMapping("/{jobId}")
    public JobPostingResponse getJobPosting(@PathVariable Long jobId) {
        JobPosting jobPosting = jobPostingService.getById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
        return toJobPostingResponse(jobPosting);
    }

    private JobPostingResponse toJobPostingResponse(JobPosting jobPosting) {
        JobPostingResponse response = new JobPostingResponse();
        response.setId(jobPosting.getId());
        response.setCompanyName(jobPosting.getCompanyName());
        response.setJobTitle(jobPosting.getJobTitle());
        response.setDeadline(jobPosting.getDeadline());
        response.setOriginalUrl(jobPosting.getOriginalUrl());
        response.setParsedData(jobPosting.getParsedData());
        response.setDescription(jobPosting.getDescription());
        response.setDescriptionRaw(jobPosting.getDescriptionRaw());
        response.setLocation(jobPosting.getLocation());
        response.setCreatedAt(jobPosting.getCreatedAt());
        response.setUpdatedAt(jobPosting.getUpdatedAt());
        return response;
    }
}
