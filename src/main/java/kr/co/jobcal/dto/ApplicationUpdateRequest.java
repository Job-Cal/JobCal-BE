package kr.co.jobcal.dto;

import java.time.LocalDate;
import kr.co.jobcal.entity.ApplicationStatus;

public class ApplicationUpdateRequest {
    private ApplicationStatus status;
    private String memo;
    private LocalDate deadline;

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

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
