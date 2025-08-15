package dev.wgrgwg.somniverse.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Provider;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.request.MemberRoleUpdateRequest;
import dev.wgrgwg.somniverse.member.dto.request.SignupRequest;
import dev.wgrgwg.somniverse.member.dto.response.MemberAdminResponse;
import dev.wgrgwg.somniverse.member.dto.response.MemberResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private MemberService memberService;

    private Member testMember;

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTests {

        @Test
        @DisplayName("회원가입 시 중복된 이메일이면 예외 발생")
        void signup_whenEmailAlreadyExists_shouldThrowException() {
            // given
            SignupRequest dto = new SignupRequest("user1", "user@email.com", "password");
            when(memberRepository.existsByEmailAndProvider(dto.email(), Provider.LOCAL)).thenReturn(
                true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(dto)).isInstanceOf(CustomException.class)
                .hasMessage(MemberErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
        }

        @Test
        @DisplayName("회원가입 시 중복된 사용자명이면 예외 발생")
        void signup_whenUsernameAlreadyExists_shouldThrowException() {
            // given
            SignupRequest dto = new SignupRequest("user1", "user@email.com", "password");
            when(memberRepository.existsByUsername(dto.username())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(dto)).isInstanceOf(CustomException.class)
                .hasMessage(MemberErrorCode.USERNAME_ALREADY_EXISTS.getMessage());
        }

        @Test
        @DisplayName("회원가입 성공 시 MemberResponseDto 반환")
        void signup_shouldReturnMemberResponseDto_whenSignupSuccess() {
            // given
            SignupRequest dto = new SignupRequest("user1", "user@email.com", "password");
            when(memberRepository.existsByEmailAndProvider(dto.email(), Provider.LOCAL)).thenReturn(
                false);
            when(memberRepository.existsByUsername(dto.username())).thenReturn(false);
            when(passwordEncoder.encode(dto.password())).thenReturn("encoded-password");

            Member savedMember = Member.builder().id(1L).username(dto.username()).email(dto.email())
                .password("encoded-password").role(Role.USER).build();

            when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

            // when
            MemberResponse responseDto = memberService.signup(dto);

            // then
            verify(memberRepository).save(any(Member.class));
            assertThat(responseDto.email()).isEqualTo(dto.email());
            assertThat(responseDto.username()).isEqualTo(dto.username());
            assertThat(responseDto.role()).isEqualTo(Role.USER.toString());
        }
    }

    @Nested
    @DisplayName("관리자 기능 테스트")
    class AdminFunctionTests {

        private Pageable pageable;
        private Member testMember;
        private Member anotherMember;

        @BeforeEach
        void setup() {
            pageable = PageRequest.of(0, 10);

            testMember = Member.builder().id(1L).username("testuser").email("test@email.com")
                .role(Role.USER).build();

            anotherMember = Member.builder().id(2L).username("another").email("another@email.com")
                .role(Role.USER).build();
        }

        @Test
        @DisplayName("관리자 회원 목록 조회 성공 시 MemberAdminResponse Page 반환")
        void getAllMembersForAdmin_whenNoKeyword_shouldReturnPagedResponse() {
            // given
            List<Member> memberList = List.of(testMember, anotherMember);
            Page<Member> memberPage = new PageImpl<>(memberList, pageable, memberList.size());
            when(memberRepository.findAll(pageable)).thenReturn(memberPage);

            // when
            Page<MemberAdminResponse> resultPage = memberService.getAllMembersForAdmin(pageable,
                null);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent().get(0).username()).isEqualTo("testuser");
            verify(memberRepository, times(1)).findAll(pageable);
            verify(memberRepository, never()).findByEmailContainingOrUsernameContaining(anyString(),
                anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("검색어로 관리자 회원 목록 조회 성공 시 MemberAdminResponse Page 반환")
        void getAllMembersForAdmin_whenWithKeyword_shouldReturnPagedResponse() {
            // given
            String keyword = "test";
            List<Member> memberList = List.of(testMember);
            Page<Member> memberPage = new PageImpl<>(memberList, pageable, memberList.size());
            when(memberRepository.findByEmailContainingOrUsernameContaining(keyword, keyword,
                pageable)).thenReturn(memberPage);

            // when
            Page<MemberAdminResponse> resultPage = memberService.getAllMembersForAdmin(pageable,
                keyword);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).username()).isEqualTo("testuser");
            verify(memberRepository, never()).findAll(any(Pageable.class));
            verify(memberRepository, times(1)).findByEmailContainingOrUsernameContaining(keyword,
                keyword, pageable);
        }

        @Test
        @DisplayName("관리자 회원 단건 조회 성공 시 MemberAdminResponse 반환")
        void getMemberForAdmin_whenMemberExists_shouldReturnResponse() {
            // given
            when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));

            // when
            MemberAdminResponse response = memberService.getMemberForAdmin(testMember.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(testMember.getId());
        }

        @Test
        @DisplayName("관리자 회원 역할 변경 성공 시 MemberAdminResponse 반환")
        void updateMemberRoleByAdmin_whenMemberExists_shouldUpdateRole() {
            // given
            MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(Role.MANAGER);
            when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));

            // when
            MemberAdminResponse response = memberService.updateMemberRoleByAdmin(testMember.getId(),
                request);

            // then
            assertThat(testMember.getRole()).isEqualTo(Role.MANAGER);
            assertThat(response.role()).isEqualTo(Role.MANAGER.toString());
        }

        @Test
        @DisplayName("역할 변경 시 존재하지 않는 회원이면 예외 발생")
        void updateMemberRoleByAdmin_whenMemberNotFound_shouldThrowException() {
            // given
            Long nonExistentMemberId = 999L;
            MemberRoleUpdateRequest request = new MemberRoleUpdateRequest(Role.MANAGER);
            when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.updateMemberRoleByAdmin(nonExistentMemberId,
                request)).isInstanceOf(CustomException.class)
                .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }
}