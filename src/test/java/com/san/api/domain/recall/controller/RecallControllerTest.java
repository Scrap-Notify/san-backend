package com.san.api.domain.recall.controller;

import com.san.api.domain.recall.dto.response.RecallQuizGenerateResponse;
import com.san.api.domain.recall.dto.response.RecallQuizResponse;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.service.RecallQuizGenerationService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecallController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecallQuizGenerationService recallQuizGenerationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void generateReturnsRecallQuizzes() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID scrapId = UUID.randomUUID();
        LocalDate targetDate = LocalDate.of(2026, 5, 19);
        RecallQuizGenerateResponse response = new RecallQuizGenerateResponse(
                targetDate,
                RecallQuizType.OX,
                List.of(new RecallQuizResponse(
                        quizId,
                        scrapId,
                        RecallQuizType.OX,
                        "React.memo는 모든 컴포넌트에 권장된다.",
                        false,
                        null,
                        null,
                        null
                ))
        );

        when(recallQuizGenerationService.generate(eq(userId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/recall/quizzes")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetDate": "2026-05-19",
                                  "quizType": "OX"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.targetDate").value("2026-05-19"))
                .andExpect(jsonPath("$.data.quizType").value("OX"))
                .andExpect(jsonPath("$.data.quizzes[0].quizId").value(quizId.toString()))
                .andExpect(jsonPath("$.data.quizzes[0].scrapId").value(scrapId.toString()))
                .andExpect(jsonPath("$.data.quizzes[0].question").value("React.memo는 모든 컴포넌트에 권장된다."))
                .andExpect(jsonPath("$.data.quizzes[0].solved").value(false));

        verify(recallQuizGenerationService).generate(eq(userId), any());
    }

    @Test
    void generateRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/recall/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void generateRejectsMissingAuthentication() {
        RecallController controller = new RecallController(recallQuizGenerationService);

        assertThatThrownBy(() -> controller.generate(null, null))
                .isInstanceOf(BusinessException.class);
    }
}
