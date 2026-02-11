package kr.co.jobcal.controller;

import java.util.List;
import java.util.stream.Collectors;
import kr.co.jobcal.dto.ApplicationResponse;
import kr.co.jobcal.dto.ApplicationUpdateRequest;
import kr.co.jobcal.dto.JobPostingResponse;
import kr.co.jobcal.entity.Application;
import kr.co.jobcal.entity.JobPosting;
import kr.co.jobcal.service.ApplicationService;
import kr.co.jobcal.service.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CurrentUserProvider currentUserProvider;

    public ApplicationController(ApplicationService applicationService, CurrentUserProvider currentUserProvider) {
        this.applicationService = applicationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<ApplicationResponse> getApplications() {
        List<Application> applications = applicationService.getUserApplications(currentUserProvider.getCurrentUserId());
        return applications.stream().map(this::toApplicationResponse).collect(Collectors.toList());
    }

    @GetMapping("/{applicationId}")
    public ApplicationResponse getApplication(@PathVariable Long applicationId) {
        Application application = applicationService.getApplication(applicationId, currentUserProvider.getCurrentUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return toApplicationResponse(application);
    }

    @PatchMapping("/{applicationId}/status")
    public ApplicationResponse updateApplicationStatus(
        @PathVariable Long applicationId,
        @RequestBody ApplicationUpdateRequest updateRequest
    ) {
        Application application = applicationService.updateApplication(
            applicationId,
            currentUserProvider.getCurrentUserId(),
            updateRequest
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return toApplicationResponse(application);
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse deleteApplication(@PathVariable Long applicationId) {
        boolean success = applicationService.deleteApplication(applicationId, currentUserProvider.getCurrentUserId());
        if (!success) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
        return new MessageResponse("Application deleted successfully");
    }

    private ApplicationResponse toApplicationResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setUserId(application.getUser().getUserId());
        response.setJobPostingId(application.getJobPosting().getId());
        response.setStatus(application.getStatus());
        response.setMemo(application.getMemo());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        response.setJobPosting(toJobPostingResponse(application.getJobPosting()));
        return response;
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
        response.setLocation(jobPosting.getLocation());
        response.setCreatedAt(jobPosting.getCreatedAt());
        response.setUpdatedAt(jobPosting.getUpdatedAt());
        return response;
    }

    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
