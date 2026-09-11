package com.example.guestbook.message;

import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import com.example.guestbook.config.AppProperties;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class MessageService {

    private final MessageRepository repository;
    private final AppProperties appProperties;

    public MessageService(MessageRepository repository, AppProperties appProperties) {
        this.repository = repository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
            .map(MessageResponse::from)
            .toList();
    }

    @Transactional
    public MessageResponse create(CreateMessageRequest request) {
        if (!appProperties.getSubmissionPasscode().equals(request.passcode())) {
            throw new InvalidPasscodeException();
        }
        Message saved = repository.save(
            new Message(request.name().trim(), request.message().trim(), OffsetDateTime.now()));
        return MessageResponse.from(saved);
    }
    
}
