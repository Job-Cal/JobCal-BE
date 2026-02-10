package kr.co.jobcal.service;

import kr.co.jobcal.entity.User;
import kr.co.jobcal.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            User user = new User();
            user.setEmail("dev@jobcal.local");
            user.setUsername("dev");
            user.setHashedPassword("dev");
            user.setIsActive(true);
            userRepository.save(user);
        }
    }
}
