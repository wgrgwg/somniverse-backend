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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.dream.dto.request.DreamCreateRequest;
import dev.wgrgwg.somniverse.dream.dto.request.DreamUpdateRequest;
import dev.wgrgwg.somniverse.dream.dto.response.DreamResponse;
import dev.wgrgwg.somniverse.dream.dto.response.DreamSimpleResponse;
import dev.wgrgwg.somniverse.dream.exception.DreamErrorCode;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.domain.Role;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = DreamController.class)
@Import(SecurityConfig.class)
class DreamControllerTest {

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

    @Nested
    @DisplayName("꿈일기 생성 api 테스트")
    @WithMockCustomUser
    class CreateDreamApiTests {

        @Test
        @DisplayName("꿈일기 생성 성공 시 201 CREATED, 생성된 꿈일기 DreamResponse 반환")
        void createDream_success_test() throws Exception {
            // given
            DreamCreateRequest request = new DreamCreateRequest("꿈일기 1", "내용", LocalDate.now(),
                true);
            MemberResponse memberResponse = new MemberResponse(1L, "testuser@email.com", "testuser",
                Role.USER.toString(), LocalDateTime.now());
            DreamResponse response = DreamResponse.builder()
                .id(1L)
                .title("꿈일기 1")
                .content("내용")
                .author(memberResponse)
                .createdAt(LocalDateTime.now())
                .build();

            when(dreamService.createDream(any(DreamCreateRequest.class), anyLong())).thenReturn(
                response);

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/dreams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

            // then
            resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("꿈일기 1"))
                .andExpect(jsonPath("$.data.author.username").value("testuser"))
            ;

            verify(dreamService, times(1)).createDream(any(DreamCreateRequest.class), anyLong());
        }
    }

    @Nested
    @DisplayName("꿈일기 수정 api 테스트")
    @WithMockCustomUser
    class UpdateDreamApiTests {

        @Test
        @DisplayName("꿈일기 수정 성공 시 200 OK, 수정된 꿈일기 DreamResponse 반환")
        void updateDream_success_test() throws Exception {
            // given
            long dreamId = 101L;
            DreamUpdateRequest request = new DreamUpdateRequest("수정된 꿈", "수정된 내용", LocalDate.now(),
                false);
            MemberResponse memberResponse = new MemberResponse(1L, "testuser@email.com", "testuser",
                Role.USER.toString(), LocalDateTime.now());
            DreamResponse dreamResponse = new DreamResponse(dreamId, "수정된 꿈",
                "수정된 내용", LocalDate.now(), false, LocalDateTime.now(), LocalDateTime.now(),
                memberResponse, false);

            when(dreamService.updateDream(anyLong(), anyLong(),
                any(DreamUpdateRequest.class))).thenReturn(dreamResponse);

            // when
            ResultActions resultActions = mockMvc.perform(put("/api/dreams/{dreamId}", dreamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(dreamId))
                .andExpect(jsonPath("$.data.title").value("수정된 꿈"))
                .andExpect(jsonPath("$.data.isPublic").value(false));
        }
    }

    @Nested
    @DisplayName("꿈일기 삭제 api 테스트")
    @WithMockCustomUser
    class DeleteDreamApiTests {

        @Test
        @DisplayName("꿈일기 삭제 성공 시 204 NO CONTENT")
        void deleteDream_success_test() throws Exception {
            // given
            long dreamId = 101L;
            doNothing().when(dreamService).deleteDream(anyLong(), anyLong());

            // when
            ResultActions resultActions = mockMvc.perform(delete("/api/dreams/{dreamId}", dreamId));

            // then
            resultActions.andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("공개 꿈일기 조회 api 테스트")
    @WithMockCustomUser
    class GetPublicDreamApiTests {

        @Test
        @DisplayName("공개 꿈일기 목록 조회 성공 시 200 OK, Page 정보 반환")
        void getPublicDreams_success_test() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<DreamSimpleResponse> content = List.of(
                new DreamSimpleResponse(101L, "공개 꿈 1", LocalDate.now(), LocalDateTime.now(),
                    "user1"));
            Page<DreamSimpleResponse> responsePage = new PageImpl<>(content, pageable, 1);

            when(dreamService.getPublicDreams(any(Pageable.class))).thenReturn(responsePage);

            // when
            ResultActions resultActions = mockMvc.perform(get("/api/dreams")
                .param("page", "0").param("size", "10"));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("공개 꿈일기 단건 조회 성공 시 200 OK, 꿈일기 DreamResponse 반환")
        void getPublicDream_success_test() throws Exception {
            // given
            long dreamId = 101L;
            MemberResponse author = new MemberResponse(2L, "other@email.com", "otheruser", "USER",
                LocalDateTime.now());
            DreamResponse dreamResponse = new DreamResponse(dreamId, "공개 꿈", "내용", LocalDate.now(),
                true, LocalDateTime.now(), LocalDateTime.now(), author, false);

            when(dreamService.getDreamWithAccessControl(anyLong(), anyLong(),
                anyBoolean())).thenReturn(dreamResponse);

            // when
            ResultActions resultActions = mockMvc.perform(get("/api/dreams/{dreamId}", dreamId));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(dreamId))
                .andExpect(jsonPath("$.data.isPublic").value(true));
        }

        @Test
        @DisplayName("비공개 꿈일기 단건 조회 실패 시 403 FORBIDDEN, 에러 응답 반환")
        void getPrivateDream_fail_test() throws Exception {
            // given
            long dreamId = 102L;
            when(dreamService.getDreamWithAccessControl(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(new CustomException(DreamErrorCode.DREAM_FORBIDDEN));

            // when
            ResultActions resultActions = mockMvc.perform(get("/api/dreams/{dreamId}", dreamId));

            // then
            resultActions.andExpect(
                    status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(DreamErrorCode.DREAM_FORBIDDEN.getCode()));
        }

        @Test
        @DisplayName("특정 사용자의 공개 꿈일기 목록 조회 성공 시 200 OK, Page 정보 반환")
        void getPublicDreamsByMember_success_test() throws Exception {
            // given
            long memberId = 2L;
            Pageable pageable = PageRequest.of(0, 10);
            List<DreamSimpleResponse> content = List.of(
                new DreamSimpleResponse(103L, "다른 사용자 꿈", LocalDate.now(), LocalDateTime.now(),
                    "otheruser"));
            Page<DreamSimpleResponse> responsePage = new PageImpl<>(content, pageable, 1);

            when(dreamService.getPublicDreamsByMember(anyLong(), any(Pageable.class))).thenReturn(
                responsePage);

            // when
            ResultActions resultActions = mockMvc.perform(
                get("/api/dreams/member/{memberId}", memberId)
                    .param("page", "0").param("size", "10"));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].authorUsername").value("otheruser"));
        }
    }

    @Nested
    @DisplayName("내 꿈일기 조회 api 테스트")
    @WithMockCustomUser
    class GetMyDreamApiTests {

        @Test
        @DisplayName("내 꿈일기 목록 조회 성공 시 200 OK, Page 정보 반환")
        void getMyDreams_success_test() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<DreamSimpleResponse> content = List.of(
                new DreamSimpleResponse(101L, "나의 공개 꿈", LocalDate.now(), LocalDateTime.now(),
                    "testuser"),
                new DreamSimpleResponse(102L, "나의 비공개 꿈", LocalDate.now(), LocalDateTime.now(),
                    "testuser")
            );
            Page<DreamSimpleResponse> responsePage = new PageImpl<>(content, pageable, 2);
            when(dreamService.getMyDreams(anyLong(), any(Pageable.class))).thenReturn(
                responsePage);

            // when
            ResultActions resultActions = mockMvc.perform(get("/api/dreams/me")
                .param("page", "0").param("size", "10"));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        }
    }
}