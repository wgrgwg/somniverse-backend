package dev.wgrgwg.somniverse.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.comment.service.CommentService;
import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.dream.service.DreamService;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.request.MemberRoleUpdateRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberAdminResponse;
import dev.wgrgwg.somniverse.member.repository.AccessTokenBlackListRepository;
import dev.wgrgwg.somniverse.member.service.AuthService;
import dev.wgrgwg.somniverse.member.service.MemberService;
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

@WebMvcTest(AdminMemberController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
public class AdminMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private DreamService dreamService;

    @MockitoBean
    private MemberService memberService;

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

    @Nested
    @DisplayName("관리자 회원 조회 API 테스트")
    class DeleteCommentByAdminApiTests {

        @Nested
        @DisplayName("관리자 회원 목록 조회 API 테스트")
        class GetAllMemberApiTests {

            @Test
            @WithMockCustomUser(role = "ADMIN")
            @DisplayName("관리자 회원 목록 조회 성공 시 200 OK, 페이지 정보 반환")
            void getAllMember_success_test() throws Exception {
                // given
                Pageable pageable = PageRequest.of(0, 10);
                List<MemberAdminResponse> content = List.of(
                    new MemberAdminResponse(1L, "user1@email.com", "user1", Role.USER.toString(),
                        LocalDateTime.now(), LocalDateTime.now(), false, null)
                );
                Page<MemberAdminResponse> responsePage = new PageImpl<>(content, pageable, 1);

                when(memberService.getAllMembersForAdmin(any(Pageable.class), any())).thenReturn(
                    responsePage);

                // when
                ResultActions resultActions = mockMvc.perform(get("/api/admin/members")
                    .param("page", "0")
                    .param("size", "10")
                    .param("keyword", "user"));

                // then
                resultActions.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
                verify(memberService, times(1)).getAllMembersForAdmin(any(Pageable.class), any());
            }

            @Test
            @WithMockCustomUser(role = "USER")
            @DisplayName("일반 사용자가 회원 목록 조회 시 403 FORBIDDEN, 에러 응답 반환")
            void getAllMember_fail_whenNotAdmin_test() throws Exception {
                // given & when
                ResultActions resultActions = mockMvc.perform(get("/api/admin/members"));

                // then
                resultActions.andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("관리자 회원 단건 조회 API 테스트")
        class GetMemberApiTests {

            @Test
            @WithMockCustomUser(role = "ADMIN")
            @DisplayName("관리자 특정 회원 조회 성공 시 200 OK, 회원 정보 반환")
            void getMember_success_test() throws Exception {
                // given
                long memberId = 1L;
                MemberAdminResponse response = new MemberAdminResponse(memberId, "user1@email.com",
                    "user1", Role.USER.toString(), LocalDateTime.now(), LocalDateTime.now(), false,
                    null);
                when(memberService.getMemberForAdmin(anyLong())).thenReturn(response);

                // when
                ResultActions resultActions = mockMvc.perform(
                    get("/api/admin/members/{memberId}", memberId));

                // then
                resultActions.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(memberId))
                    .andExpect(jsonPath("$.data.email").value("user1@email.com"));
                verify(memberService, times(1)).getMemberForAdmin(anyLong());
            }

            @Test
            @WithMockCustomUser(role = "USER")
            @DisplayName("일반 사용자가 회원 단건 조회 시 403 FORBIDDEN, 에러 응답 반환")
            void getMember_fail_whenNotAdmin_test() throws Exception {
                // given
                long memberId = 1L;
                MemberAdminResponse response = new MemberAdminResponse(memberId, "user1@email.com",
                    "user1", Role.USER.toString(), LocalDateTime.now(), LocalDateTime.now(), false,
                    null);
                when(memberService.getMemberForAdmin(anyLong())).thenReturn(response);

                // when
                ResultActions resultActions = mockMvc.perform(
                    get("/api/admin/members/{memberId}", memberId));

                // then
                resultActions.andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("관리자 회원 역할 변경 API 테스트")
    class UpdateMemberRoleApiTests {

        @Test
        @WithMockCustomUser(role = "ADMIN")
        @DisplayName("관리자 회원 역할 변경 성공 시 200 OK, 변경된 회원 정보 반환")
        void updateMemberRole_success_test() throws Exception {
            // given
            long memberId = 1L;
            MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(Role.ADMIN);
            MemberAdminResponse response = new MemberAdminResponse(memberId, "user1@email.com",
                "user1", Role.ADMIN.toString(), LocalDateTime.now(), LocalDateTime.now(), false,
                null);

            when(memberService.updateMemberRoleByAdmin(anyLong(),
                any(MemberRoleUpdateRequest.class))).thenReturn(response);

            // when
            ResultActions resultActions = mockMvc.perform(
                patch("/api/admin/members/{memberId}/role", memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value(Role.ADMIN.toString()));
            verify(memberService, times(1)).updateMemberRoleByAdmin(anyLong(),
                any(MemberRoleUpdateRequest.class));
        }

        @Test
        @WithMockCustomUser(role = "ADMIN")
        @DisplayName("요청 DTO 유효성 검증 실패 시 400 BAD REQUEST, 에러 응답 반환")
        void updateMemberRole_fail_whenInvalidRequest_test() throws Exception {
            // given
            long memberId = 1L;
            // 유효하지 않은 요청 (role이 null)
            MemberRoleUpdateRequest invalidRequest = new MemberRoleUpdateRequest(null);

            // when
            ResultActions resultActions = mockMvc.perform(
                patch("/api/admin/members/{memberId}/role", memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)));

            // then
            resultActions.andExpect(status().isBadRequest());
        }

        @Test
        @WithMockCustomUser(role = "MANAGER")
        @DisplayName("ADMIN이 아닌 사용자가 회원 권한 수정 시 403 FORBIDDEN 에러 응답 반환")
        void updateMemberRole_fail_whenNotAdmin_test() throws Exception {
            // given
            long memberId = 1L;
            MemberRoleUpdateRequest invalidRequest = new MemberRoleUpdateRequest(null);

            // when
            ResultActions resultActions = mockMvc.perform(
                patch("/api/admin/members/{memberId}/role", memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)));

            // then
            resultActions.andExpect(status().isForbidden());
        }
    }
}
