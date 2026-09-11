package com.example.guestbook.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.guestbook.config.AppProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// WebConfig (a WebMvcConfigurer) is pulled into this slice and needs AppProperties,
// which @ConfigurationPropertiesScan doesn't run for a @WebMvcTest slice — enable it explicitly.
@EnableConfigurationProperties(AppProperties.class)
@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MessageService service;

    @Test
    void GET_lists_messages_without_the_passcode() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new MessageResponse(1L, "Ada", "hi", OffsetDateTime.parse("2026-01-01T00:00:00Z"))));

        mvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ada"))
                .andExpect(jsonPath("$[0].passcode").doesNotExist());
    }

    @Test
    void POST_with_a_blank_name_is_400() throws Exception {
        mvc.perform(post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","message":"hi","passcode":"let-me-in"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_with_a_wrong_passcode_is_403() throws Exception {
        when(service.create(any())).thenThrow(new InvalidPasscodeException());

        mvc.perform(post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","message":"hi","passcode":"nope"}"""))
                .andExpect(status().isForbidden());
    }
}