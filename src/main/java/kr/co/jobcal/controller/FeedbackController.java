package kr.co.jobcal.controller;

import kr.co.jobcal.dto.FeedbackCreateRequest;
import kr.co.jobcal.entity.Feedback;
import kr.co.jobcal.service.CurrentUserProvider;
import kr.co.jobcal.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUserProvider currentUserProvider;

    public FeedbackController(FeedbackService feedbackService, CurrentUserProvider currentUserProvider) {
        this.feedbackService = feedbackService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse createFeedback(@RequestBody FeedbackCreateRequest request) {
        try {
            Feedback saved = feedbackService.create(currentUserProvider.getCurrentUserId(), request);
            return new MessageResponse("Feedback submitted", saved.getId());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public static class MessageResponse {
        private String message;
        private Long id;

        public MessageResponse(String message, Long id) {
            this.message = message;
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
