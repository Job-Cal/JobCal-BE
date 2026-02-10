package kr.co.jobcal.dto;

public class JobPostingParseResponse {
    private boolean success;
    private JobPostingCreateRequest data;
    private String error;

    public JobPostingParseResponse() {
    }

    public JobPostingParseResponse(boolean success, JobPostingCreateRequest data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public JobPostingCreateRequest getData() {
        return data;
    }

    public void setData(JobPostingCreateRequest data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
