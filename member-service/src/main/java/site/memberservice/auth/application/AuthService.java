package site.memberservice.auth.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import site.memberservice.auth.application.dto.LoginCommand;
import site.memberservice.auth.application.dto.LoginResult;
import site.memberservice.auth.domain.AuthToken;
import site.memberservice.auth.domain.AuthTokenProvider;
import site.memberservice.auth.domain.RefreshToken;
import site.memberservice.auth.domain.repository.RefreshTokenRepository;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.domain.Member;

import static java.lang.String.format;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenProvider authTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResult login(final LoginCommand command) {
        final Member member = memberService.findMember(command.email())
            .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS, format("존재하지 않는 회원의 이메일입니다. input: %s", command.email())));

        if (!passwordEncoder.matches(command.password(), member.getPassword())) {
            throw new AuthException(INVALID_CREDENTIALS, "유효하지 않는 회원 비밀번호입니다.");
        }

        final AuthToken accessToken = authTokenProvider.createAccessToken(member.getId());
        final RefreshToken refreshToken = RefreshToken.create(authTokenProvider.createRefreshToken(member.getId()).getValue(), member.getId());
        refreshTokenRepository.deleteAllByMemberId(member.getId());
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(accessToken.getValue(), refreshToken.getValue());
    }

    @Transactional
    public void logout(final Long memberId) {
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }
}
