package kr.co.jobcal.repository;

import java.util.Optional;
import kr.co.jobcal.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    Optional<JobPosting> findByOriginalUrl(String originalUrl);
}
