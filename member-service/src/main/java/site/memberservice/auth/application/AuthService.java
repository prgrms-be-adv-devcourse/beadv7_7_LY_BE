package site.memberservice.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import site.memberservice.auth.application.dto.LoginCommand;
import site.memberservice.auth.application.dto.LoginResult;
import site.memberservice.auth.domain.AuthTokenProvider;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.domain.Member;

import static java.lang.String.*;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;

@RequiredArgsConstructor
@Service
public class AuthService {

    // TODO : #80 회원 모듈과 인중 모듈 분리를 고려해 파이널에서 회원 API를 호출하는 방식으로 변경
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenProvider authTokenProvider;

    public LoginResult login(final LoginCommand command) {
        final Member member = memberService.findMember(command.email())
            .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS, format("존재하지 않는 회원의 이메일입니다. input: %s", command.email())));

        if (!passwordEncoder.matches(command.password(), member.getPassword())) {
            throw new AuthException(INVALID_CREDENTIALS, "유효하지 않는 회원 비밀번호입니다.");
        }

        return new LoginResult(authTokenProvider.createToken(member.getId()).getValue());
    }

    // TODO : #80 로그아웃은 파이널에서 Refresh 토큰 정책 결정 + Access Token 관리 정책이 고도화되면 함께 구현 예정
    public void logout() {

    }
}
