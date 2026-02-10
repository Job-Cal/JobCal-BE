package kr.co.jobcal.dto;

import kr.co.jobcal.entity.ApplicationStatus;

public class ApplicationUpdateRequest {
    private ApplicationStatus status;
    private String memo;

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
