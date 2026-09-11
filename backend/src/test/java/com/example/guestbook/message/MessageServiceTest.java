package com.example.guestbook.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.guestbook.config.AppProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    MessageRepository repository;

    AppProperties appProperties;
    MessageService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setSubmissionPasscode("let-me-in");
        service = new MessageService(repository, appProperties);
    }

    @Test
    void create_rejects_a_wrong_passcode() {
        var request = new CreateMessageRequest("Ada", "hello", "wrong");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidPasscodeException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_trims_name_and_body() {
        when(repository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new CreateMessageRequest("  Ada  ", "  hello  ", "let-me-in"));

        var saved = ArgumentCaptor.forClass(Message.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ada");
        assertThat(saved.getValue().getBody()).isEqualTo("hello");
    }

    @Test
    void findAll_maps_entities_to_responses() {
        when(repository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(new Message("Ada", "first", OffsetDateTime.now())));

        List<MessageResponse> result = service.findAll();

        assertThat(result).singleElement()
                .satisfies(r -> assertThat(r.name()).isEqualTo("Ada"));
    }
}