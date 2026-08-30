package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.repository.MemberAddressView;
import site.memberservice.member.domain.repository.MemberCredentials;
import site.memberservice.member.domain.repository.MemberProfileView;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.email.hash = :hash")
    Optional<Member> findByEmailHash(@Param("hash") String hash);

    boolean existsByNickname(String nickName);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.email.hash = :hash")
    boolean existsByEmailHash(@Param("hash") String hash);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.phoneNumber.hash = :hash")
    boolean existsByPhoneNumberHash(@Param("hash") String hash);

    @Query("SELECT new site.memberservice.member.domain.repository.MemberCredentials(m.id, m.password) FROM Member m WHERE m.email.hash = :hash")
    Optional<MemberCredentials> findCredentialsByEmailHash(@Param("hash") String hash);

    @Query("SELECT m.nickname FROM Member m WHERE m.id = :id")
    Optional<String> findNicknameById(@Param("id") Long id);

    @Query("SELECT new site.memberservice.member.domain.repository.MemberProfileView(m.id, m.email.value, m.nickname) FROM Member m WHERE m.id = :id")
    Optional<MemberProfileView> findProfileById(@Param("id") Long id);

    @Query("SELECT new site.memberservice.member.domain.repository.MemberAddressView(m.address.zipcode, m.address.baseAddress, m.address.detailAddress) FROM Member m WHERE m.id = :id")
    Optional<MemberAddressView> findAddressViewById(@Param("id") Long id);

    @Query("SELECT m.name FROM Member m WHERE m.id = :id")
    Optional<String> findNameById(@Param("id") Long id);
}
