package kr.co.jobcal.repository;

import java.util.Optional;
import kr.co.jobcal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderSubject(String providerSubject);
    boolean existsByUserId(String userId);
}
