package kr.co.jobcal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobCalApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobCalApplication.class, args);
    }

}
