package kr.co.jobcal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.co.jobcal.entity.Application;
import kr.co.jobcal.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @EntityGraph(attributePaths = {"jobPosting"})
    List<Application> findByUserUserIdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = {"jobPosting"})
    Optional<Application> findByIdAndUserUserId(Long id, String userId);

    @EntityGraph(attributePaths = {"jobPosting"})
    @Query("select a from Application a join a.jobPosting jp where a.user.userId = :userId and jp.deadline between :start and :end")
    List<Application> findByUserUserIdAndDeadlineBetween(
        @Param("userId") String userId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @EntityGraph(attributePaths = {"jobPosting"})
    @Query("select a from Application a join a.jobPosting jp where jp.deadline between :start and :end and a.status not in :excluded")
    List<Application> findByDeadlineBetweenExcludingStatus(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("excluded") List<ApplicationStatus> excluded
    );
}
