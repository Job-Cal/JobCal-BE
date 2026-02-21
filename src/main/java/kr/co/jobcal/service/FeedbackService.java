package kr.co.jobcal.service;

import kr.co.jobcal.dto.FeedbackCreateRequest;
import kr.co.jobcal.entity.Feedback;
import kr.co.jobcal.entity.User;
import kr.co.jobcal.repository.FeedbackRepository;
import kr.co.jobcal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Feedback create(String userId, FeedbackCreateRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message is required");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setCategory(normalize(request.getCategory(), 50));
        feedback.setPagePath(normalize(request.getPagePath(), 200));
        feedback.setMessage(request.getMessage().trim());

        return feedbackRepository.save(feedback);
    }

    private String normalize(String value, int maxLen) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }
}
