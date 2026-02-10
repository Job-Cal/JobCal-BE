package kr.co.jobcal.dto;

import kr.co.jobcal.entity.ApplicationStatus;

public class ApplicationCreateRequest {
    private Long jobPostingId;
    private ApplicationStatus status = ApplicationStatus.NOT_APPLIED;
    private String memo;

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
}
