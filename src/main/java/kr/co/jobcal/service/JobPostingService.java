package kr.co.jobcal.service;

import java.util.Optional;
import kr.co.jobcal.dto.JobPostingCreateRequest;
import kr.co.jobcal.entity.JobPosting;
import kr.co.jobcal.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;

    public JobPostingService(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    @Transactional
    public JobPosting createOrUpdate(JobPostingCreateRequest request) {
        Optional<JobPosting> existing = jobPostingRepository.findByOriginalUrl(request.getOriginalUrl());
        if (existing.isPresent()) {
            JobPosting jobPosting = existing.get();
            if (request.getDeadline() != null) {
                jobPosting.setDeadline(request.getDeadline());
                jobPosting.setCompanyName(request.getCompanyName());
                jobPosting.setJobTitle(request.getJobTitle());
                if (request.getDescription() != null) {
                    jobPosting.setDescription(request.getDescription());
                }
                if (request.getLocation() != null) {
                    jobPosting.setLocation(request.getLocation());
                }
                if (request.getParsedData() != null) {
                    jobPosting.setParsedData(request.getParsedData());
                }
            }
            return jobPostingRepository.save(jobPosting);
        }

        JobPosting jobPosting = new JobPosting();
        jobPosting.setCompanyName(request.getCompanyName());
        jobPosting.setJobTitle(request.getJobTitle());
        jobPosting.setDeadline(request.getDeadline());
        jobPosting.setOriginalUrl(request.getOriginalUrl());
        jobPosting.setParsedData(request.getParsedData());
        jobPosting.setDescription(request.getDescription());
        jobPosting.setLocation(request.getLocation());
        return jobPostingRepository.save(jobPosting);
    }

    public Optional<JobPosting> getById(Long id) {
        return jobPostingRepository.findById(id);
    }
}
