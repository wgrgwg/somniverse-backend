package dev.wgrgwg.somniverse.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.request.SignupRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.message.MemberSuccessMessage;
import dev.wgrgwg.somniverse.member.repository.AccessTokenBlackListRepository;
import dev.wgrgwg.somniverse.member.service.MemberService;
import dev.wgrgwg.somniverse.security.config.SecurityConfig;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.oauth.handler.OAuth2AuthenticationFailureHandler;
import dev.wgrgwg.somniverse.security.oauth.handler.OAuth2AuthenticationSuccessHandler;
import dev.wgrgwg.somniverse.security.oauth.service.CustomOAuth2UserService;
import dev.wgrgwg.somniverse.support.security.WithMockCustomUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = MemberController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private RefreshTokenCookieUtil refreshTokenCookieUtil;

    @MockitoBean
    private AccessTokenBlackListRepository blackListRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private SignupRequest signupRequestDto;
    private MemberResponse responseDto;

    @BeforeEach
    void init() {
        signupRequestDto = new SignupRequest(
            "user01@email.com",
            "password01!",
            "user01"
        );

        responseDto = new MemberResponse(
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
            ResultActions resultActions = mockMvc.perform(post("/api/auth/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                    jsonPath("$.message").value(MemberSuccessMessage.SIGNUP_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.email").value(signupRequestDto.email()))
                .andExpect(jsonPath("$.data.username").value(signupRequestDto.username()))
                .andExpect(jsonPath("$.data.role").value(Role.USER.toString()));
        }

        @Test
        @DisplayName("중복 이메일로 회원가입 실패 시 409 CONFLICT와 에러 응답 반환")
        void signup_fail_emailAlreadyExists_test() throws Exception {
            // given
            when(memberService.signup(signupRequestDto)).thenThrow(
                new CustomException(MemberErrorCode.EMAIL_ALREADY_EXISTS));

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                    jsonPath("$.message").value(MemberErrorCode.EMAIL_ALREADY_EXISTS.getMessage()))
                .andExpect(
                    jsonPath("$.errorCode").value(MemberErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
        }

        @Test
        @DisplayName("중복 사용자명으로 회원가입 실패 시 409 CONFLICT와 에러 응답 반환")
        void signup_fail_usernameAlreadyExists_test() throws Exception {
            // given
            when(memberService.signup(signupRequestDto)).thenThrow(
                new CustomException(MemberErrorCode.USERNAME_ALREADY_EXISTS));

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                    jsonPath("$.message").value(
                        MemberErrorCode.USERNAME_ALREADY_EXISTS.getMessage()))
                .andExpect(
                    jsonPath("$.errorCode").value(
                        MemberErrorCode.USERNAME_ALREADY_EXISTS.getCode()));
        }
    }

    @Nested
    @DisplayName("내정보 조회 api 테스트")
    @WithMockCustomUser
    class GetMyInfoApiTests {

        @Test
        @DisplayName("내 정보 조회 성공 시 200OK와 MemberResponse 반환")
        void getMyInfo_success_test() throws Exception {
            // given
            when(memberService.getMember(any())).thenReturn(responseDto);

            // when
            ResultActions resultActions = mockMvc.perform(get("/api/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequestDto)));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(responseDto.email()))
                .andExpect(jsonPath("$.data.username").value(responseDto.username()))
                .andExpect(jsonPath("$.data.role").value(Role.USER.toString()));
        }

    }
}