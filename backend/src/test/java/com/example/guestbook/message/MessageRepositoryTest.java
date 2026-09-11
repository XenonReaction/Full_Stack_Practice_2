package com.example.guestbook.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.guestbook.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MessageRepositoryTest {

    @Autowired
    MessageRepository repository;

    @Test
    void findAllByOrderByCreatedAtDesc_returns_newest_first() {
        repository.save(new Message("Ada", "older", OffsetDateTime.now().minusHours(1)));
        repository.save(new Message("Grace", "newer", OffsetDateTime.now()));

        assertThat(repository.findAllByOrderByCreatedAtDesc())
                .extracting(Message::getName)
                .containsExactly("Grace", "Ada");
    }
}