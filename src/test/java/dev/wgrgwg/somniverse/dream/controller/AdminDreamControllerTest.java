package dev.wgrgwg.somniverse.dream.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.dto.response.DreamSimpleResponse;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.idempotency.filter.IdempotencyFilter;
import dev.wgrgwg.somniverse.global.idempotency.store.IdempotencyRepository;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import dev.wgrgwg.somniverse.member.repository.AccessTokenBlackListRepository;
import dev.wgrgwg.somniverse.member.service.AuthService;
import dev.wgrgwg.somniverse.security.config.SecurityConfig;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.oauth.handler.OAuth2AuthenticationFailureHandler;
import dev.wgrgwg.somniverse.security.oauth.handler.OAuth2AuthenticationSuccessHandler;
import dev.wgrgwg.somniverse.security.oauth.service.CustomOAuth2UserService;
import dev.wgrgwg.somniverse.support.security.WithMockCustomUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = AdminDreamController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DreamService dreamService;

    @MockitoBean
    private AuthService authService;

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

    @MockitoBean
    private IdempotencyRepository idempotencyRepository;

    @MockitoBean
    private IdempotencyFilter idempotencyFilter;

    @Nested
    @DisplayName("관리자 권한 꿈일기 조회 api 테스트")
    class GetDreamAsAdminApiTests {

        @Test
        @WithMockCustomUser(role = "ADMIN")
        @DisplayName("꿈일기 전체 조회 성공 시 200 OK, Page 정보 반환")
        void getAllDreams_forAdmin_success_test() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<DreamSimpleResponse> content = List.of(
                new DreamSimpleResponse(1L, "꿈1", LocalDate.now(), LocalDateTime.now(), "user1"),
                new DreamSimpleResponse(1L, "꿈2", LocalDate.now(), LocalDateTime.now(), "user2"),
                new DreamSimpleResponse(1L, "삭제된 꿈", LocalDate.now(), LocalDateTime.now(), "user1")
            );

            Page<DreamSimpleResponse> responsePage = new PageImpl<>(content, pageable, 3);
            when(dreamService.getAllDreamsForAdmin(any(Pageable.class), anyBoolean())).thenReturn(
                responsePage);

            // when
            ResultActions resultActions = mockMvc.perform(get("/api/admin/dreams")
                .param("page", "0").param("size", "10").param("includeDeleted", "true"));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3));
        }

        @Test
        @WithMockCustomUser(role = "ADMIN")
        @DisplayName("삭제된 꿈일기 단건 조회 성공 시 200 OK, 꿈일기 정보 반환")
        void getDeletedDream_asAdmin_success_test() throws Exception {
            // given
            long dreamId = 103L;
            MemberResponse author = new MemberResponse(1L, "test@email.com", "testuser", "USER",
                LocalDateTime.now());
            DreamResponse dreamResponse = new DreamResponse(dreamId, "삭제된 꿈", "내용", LocalDate.now(),
                false, LocalDateTime.now(), LocalDateTime.now(), author, true);
            when(dreamService.getDreamForAdmin(anyLong(), anyBoolean())).thenReturn(dreamResponse);

            // when
            ResultActions resultActions = mockMvc.perform(
                get("/api/admin/dreams/{dreamId}", dreamId)
                    .param("includeDeleted", "true"));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(dreamId))
                .andExpect(jsonPath("$.data.isDeleted").value(true));
        }
    }

    @Nested
    @DisplayName("관리자 권한 꿈일기 삭제 api 테스트")
    class DeleteDreamApiTests {

        @Test
        @WithMockCustomUser(role = "ADMIN")
        @DisplayName("관리자 권한으로 꿈일기 삭제 성공 시 204 NO CONTENT")
        void deleteDream_success_test() throws Exception {
            // given
            long dreamId = 101L;
            doNothing().when(dreamService).deleteDreamByAdmin(anyLong());

            // when
            ResultActions resultActions = mockMvc.perform(
                delete("/api/admin/dreams/{dreamId}", dreamId));

            // then
            resultActions.andExpect(status().isNoContent());
            verify(dreamService, times(1)).deleteDreamByAdmin(anyLong());
        }
    }

}