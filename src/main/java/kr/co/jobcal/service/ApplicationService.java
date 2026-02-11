package kr.co.jobcal.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.co.jobcal.dto.ApplicationCreateRequest;
import kr.co.jobcal.dto.ApplicationUpdateRequest;
import kr.co.jobcal.entity.Application;
import kr.co.jobcal.entity.JobPosting;
import kr.co.jobcal.entity.User;
import kr.co.jobcal.repository.ApplicationRepository;
import kr.co.jobcal.repository.JobPostingRepository;
import kr.co.jobcal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;

    public ApplicationService(
        ApplicationRepository applicationRepository,
        UserRepository userRepository,
        JobPostingRepository jobPostingRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    @Transactional
    public Application createApplication(String userId, ApplicationCreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        JobPosting jobPosting = jobPostingRepository.findById(request.getJobPostingId())
            .orElseThrow(() -> new IllegalArgumentException("Job posting not found"));

        Application application = new Application();
        application.setUser(user);
        application.setJobPosting(jobPosting);
        application.setStatus(request.getStatus());
        application.setMemo(request.getMemo());
        return applicationRepository.save(application);
    }

    public List<Application> getUserApplications(String userId) {
        return applicationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Application> getApplication(Long applicationId, String userId) {
        return applicationRepository.findByIdAndUserUserId(applicationId, userId);
    }

    @Transactional
    public Optional<Application> updateApplication(Long applicationId, String userId, ApplicationUpdateRequest request) {
        Optional<Application> optional = applicationRepository.findByIdAndUserUserId(applicationId, userId);
        if (optional.isEmpty()) {
            return Optional.empty();
        }

        Application application = optional.get();
        if (request.getStatus() != null) {
            application.setStatus(request.getStatus());
        }
        if (request.getMemo() != null) {
            application.setMemo(request.getMemo());
        }
        return Optional.of(applicationRepository.save(application));
    }

    public List<Application> getApplicationsByDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        return applicationRepository.findByUserUserIdAndDeadlineBetween(userId, startDate, endDate);
    }

    @Transactional
    public boolean deleteApplication(Long applicationId, String userId) {
        Optional<Application> optional = applicationRepository.findByIdAndUserUserId(applicationId, userId);
        if (optional.isEmpty()) {
            return false;
        }
        applicationRepository.delete(optional.get());
        return true;
    }
}
