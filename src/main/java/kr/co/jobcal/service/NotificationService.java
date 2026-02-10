package kr.co.jobcal.service;

import java.time.LocalDate;
import java.util.List;
import kr.co.jobcal.entity.Application;
import kr.co.jobcal.entity.ApplicationStatus;
import kr.co.jobcal.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final ApplicationRepository applicationRepository;

    public NotificationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void checkAndNotifyUpcomingDeadlines() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        List<Application> applications = applicationRepository.findByDeadlineBetweenExcludingStatus(
            today,
            threeDaysLater,
            List.of(ApplicationStatus.REJECTED, ApplicationStatus.ACCEPTED)
        );

        for (Application application : applications) {
            logger.info(
                "Upcoming deadline: {} - {} (Deadline: {})",
                application.getJobPosting().getCompanyName(),
                application.getJobPosting().getJobTitle(),
                application.getJobPosting().getDeadline()
            );
        }
    }

    public List<Application> getApplicationsNeedingNotification(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);
        return applicationRepository.findByUserIdAndDeadlineBetween(userId, today, threeDaysLater);
    }
}
