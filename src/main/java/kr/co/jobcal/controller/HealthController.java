package kr.co.jobcal.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/")
    public Map<String, String> root() {
        return Map.of("message", "JobCal API", "version", "1.0.0");
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy");
    }
}
