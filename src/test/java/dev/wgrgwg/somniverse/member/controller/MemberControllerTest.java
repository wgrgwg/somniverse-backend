package dev.wgrgwg.somniverse.member.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.config.SecurityConfig;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.MemberResponseDto;
import dev.wgrgwg.somniverse.member.dto.MemberSignupRequestDto;
import dev.wgrgwg.somniverse.member.exception.EmailAlreadyExistsException;
import dev.wgrgwg.somniverse.member.exception.UsernameAlreadyExistsException;
import dev.wgrgwg.somniverse.member.message.MemberSuccessMessage;
import dev.wgrgwg.somniverse.member.service.MemberService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = MemberController.class)
@Import(SecurityConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    private MemberSignupRequestDto signupRequestDto;
    private MemberResponseDto responseDto;

    @BeforeEach
    void init() {
        signupRequestDto = new MemberSignupRequestDto(
            "user01@email.com",
            "password01!",
            "user01"
        );

        responseDto = new MemberResponseDto(
            1L,
            "user01@email.com",
            "user01",
            "사용자",
            LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("회원가입 api 테스트")
    class SignupApiTests {

        @Test
        @DisplayName("회원가입 성공 시 201 CREATED와 MemberResponseDto 반환")
        void signup_success_test() throws Exception {
            // given
            when(memberService.signup(signupRequestDto)).thenReturn(responseDto);

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(MemberSuccessMessage.SIGNUP_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.email").value(signupRequestDto.email()))
                .andExpect(jsonPath("$.data.username").value(signupRequestDto.username()))
                .andExpect(jsonPath("$.data.role").value(Role.USER.toString()));
        }

        @Test
        @DisplayName("중복 이메일로 회원가입 실패 시 409 CONFLICT와 에러 응답 반환")
        void signup_fail_emailAlreadyExists_test() throws Exception {
            // given
            when(memberService.signup(signupRequestDto)).thenThrow(
                new EmailAlreadyExistsException());

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용중인 이메일입니다"));
        }

        @Test
        @DisplayName("중복 사용자명으로 회원가입 실패 시 409 CONFLICT와 에러 응답 반환")
        void signup_fail_usernameAlreadyExists_test() throws Exception {
            // given
            when(memberService.signup(signupRequestDto)).thenThrow(
                new UsernameAlreadyExistsException());

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용중인 사용자명입니다"));
        }
    }
}