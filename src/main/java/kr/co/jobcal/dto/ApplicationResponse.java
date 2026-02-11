package kr.co.jobcal.dto;

import java.time.LocalDateTime;
import kr.co.jobcal.entity.ApplicationStatus;

public class ApplicationResponse {
    private Long id;
    private String userId;
    private Long jobPostingId;
    private ApplicationStatus status;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private JobPostingResponse jobPosting;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getJobPostingId() {
        return jobPostingId;
    }

    public void setJobPostingId(Long jobPostingId) {
        this.jobPostingId = jobPostingId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public JobPostingResponse getJobPosting() {
        return jobPosting;
    }

    public void setJobPosting(JobPostingResponse jobPosting) {
        this.jobPosting = jobPosting;
    }
}
