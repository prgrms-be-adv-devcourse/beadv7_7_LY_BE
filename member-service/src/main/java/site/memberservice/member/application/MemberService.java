package site.memberservice.member.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.memberservice.global.crypto.hashing.Hasher;
import site.memberservice.member.application.dto.AddressDto;
import site.memberservice.member.application.dto.MemberRegisterCommand;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.member.domain.repository.MemberRepository;
import site.memberservice.member.exception.MemberException;

import static java.lang.String.format;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_MEMBER_INFO;
import static site.memberservice.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final Hasher hasher;
    private final MemberRepository memberRepository;

    // TODO : 회원 개인 정보 암호화 및 관리 정책을 반드시 고민해서 적용하기
    @Transactional
    public void register(final MemberRegisterCommand command) {
        final Email email = new Email(command.email());
        final PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber());
        final Address address = new Address(command.zipcode(), command.baseAddress(), command.detailAddress());

        validatePassword(command.password());
        validateDuplicateNickName(command.nickName());
        validateDuplicatePhoneNumber(phoneNumber);

        final String hashedPassword = hasher.hashing(command.password());
        final Member createdMember = Member.create(
            email,
            hashedPassword,
            command.nickName(),
            command.name(),
            phoneNumber,
            address
        );

        memberRepository.save(createdMember);
    }

    private void validatePassword(final String password) {
        if (!Member.isValidPassword(password)) {
            throw new MemberException(INVALID_MEMBER_INFO, format("비밀번호는 영문자, 숫자, 특수문자를 포함하여 8 ~ 16 길이의 문자열만 가능합니다. input: %s", password));
        }
    }

    private void validateDuplicateNickName(final String nickName) {
        if (memberRepository.existsByNickname(nickName)) {
            throw new MemberException(INVALID_MEMBER_INFO, format("이미 존재하는 회원 닉네임입니다. input: %s", nickName));
        }
    }

    private void validateDuplicatePhoneNumber(final PhoneNumber phoneNumber) {
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw new MemberException(INVALID_MEMBER_INFO, format("이미 존재하는 회원 전화번호입니다. input: %s", phoneNumber));
        }
    }

    public AddressDto getMemberAddress(final Long memberId) {
        final Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId)));

        return AddressDto.from(member.getAddress());
    }
}
