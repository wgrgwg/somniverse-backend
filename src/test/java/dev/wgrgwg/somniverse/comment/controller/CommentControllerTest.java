package dev.wgrgwg.somniverse.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.comment.dto.request.CommentCreateRequest;
import dev.wgrgwg.somniverse.comment.dto.request.CommentUpdateRequest;
import dev.wgrgwg.somniverse.comment.dto.response.CommentResponse;
import dev.wgrgwg.somniverse.comment.exception.CommentErrorCode;
import dev.wgrgwg.somniverse.comment.service.CommentService;
import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.exception.CustomException;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

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
    @DisplayName("댓글 생성 api 테스트")
    @WithMockCustomUser
    class CreateCommentApiTests {

        @Test
        @DisplayName("댓글 작성 성공 시 201 CREATED, CommentResponse 반환")
        void createComment_success_test() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest("댓글 내용", null);
            MemberResponse memberResponse = new MemberResponse(1L, "user01@email.com", "user01",
                "작성자", LocalDateTime.now());
            CommentResponse response = new CommentResponse(1L, "댓글 내용", memberResponse,
                LocalDateTime.now(),
                LocalDateTime.now(), false, null, null);
            when(commentService.createComment(anyLong(), any(CommentCreateRequest.class),
                anyLong())).thenReturn(response);

            // when
            ResultActions resultActions = mockMvc.perform(
                post("/api/dreams/{dreamId}/comments", 1L).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));

            verify(commentService, times(1)).createComment(anyLong(),
                any(CommentCreateRequest.class), anyLong());
        }
    }

    @Nested
    @DisplayName("댓글 수정 API 테스트")
    @WithMockCustomUser
    class UpdateCommentApiTests {

        @Test
        @DisplayName("댓글 수정 성공 시 200 OK, CommentResponse 반환")
        void updateComment_success_test() throws Exception {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글", null);
            MemberResponse memberResponse = new MemberResponse(1L, "user01@email.com", "user01",
                "작성자", LocalDateTime.now());
            CommentResponse response = new CommentResponse(1L, "수정된 댓글", memberResponse,
                LocalDateTime.now(),
                LocalDateTime.now(), false, null, null);
            when(commentService.updateComment(anyLong(), any(CommentUpdateRequest.class),
                anyLong())).thenReturn(response);

            // when
            ResultActions resultActions = mockMvc.perform(
                put("/api/comments/{commentId}", 1L).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글"));
        }

        @Test
        @DisplayName("댓글 수정 실패 시 403 FORBIDDEN, 에러 응답 반환")
        void updateComment_with_wrong_authorization_fail_test() throws Exception {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글", null);
            when(commentService.updateComment(anyLong(), any(CommentUpdateRequest.class),
                anyLong())).thenThrow(new CustomException(CommentErrorCode.COMMENT_FORBIDDEN));

            // when
            ResultActions resultActions = mockMvc.perform(
                put("/api/comments/{commentId}", 1L).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            resultActions.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("댓글 삭제 API 테스트")
    @WithMockCustomUser
    class DeleteCommentApiTests {

        @Test
        @DisplayName("댓글 삭제 성공 시 204 NO CONTENT")
        void deleteComment_success_test() throws Exception {
            // given
            doNothing().when(commentService).deleteComment(anyLong(), anyLong());

            // when
            ResultActions resultActions = mockMvc.perform(delete("/api/comments/{commentId}", 1L));

            // then
            resultActions.andExpect(status().isNoContent());
            resultActions.andDo(print());
        }

        @Test
        @DisplayName("댓글 삭제 실패 시 403 FORBIDDEN, 에러 응답 반환")
        void deleteComment_fail_test() throws Exception {
            // given
            doThrow(new CustomException(CommentErrorCode.COMMENT_FORBIDDEN)).when(commentService)
                .deleteComment(any(), any());

            // when
            ResultActions resultActions = mockMvc.perform(
                delete("/api/comments/{commentId}", 999L));

            // then
            resultActions.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("부모 댓글 조회 API 테스트")
    @WithMockCustomUser
    class GetParentCommentsApiTests {

        @Test
        @DisplayName("부모 댓글 목록 조회 성공 시 200 OK, Page 정보 반환")
        void getParentComments_success_test() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<CommentResponse> page = new PageImpl<>(List.of());
            when(commentService.getPagedParentCommentsByDream(anyLong(), anyBoolean(),
                any(Pageable.class))).thenReturn(page);

            // when
            ResultActions resultActions = mockMvc.perform(
                get("/api/dreams/{dreamId}/comments", 1L).param("page", "0").param("size", "10"));

            // then
            resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("대댓글 조회 API 테스트")
    @WithMockCustomUser
    class GetChildrenCommentsApiTests {

        @Test
        @DisplayName("대댓글 목록 조회 성공 시 200 OK, Page 정보 반환")
        void getChildrenComments_success_test() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<CommentResponse> page = new PageImpl<>(List.of());
            when(commentService.getPagedChildrenCommentsByParent(anyLong(), anyBoolean(),
                any(Pageable.class))).thenReturn(page);

            // when
            ResultActions resultActions = mockMvc.perform(
                get("/api/comments/{commentId}/children", 1L).param("page", "0")
                    .param("size", "10"));

            // then
            resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }
    }
}