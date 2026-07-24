package site.memberservice.global.crypto.hashing;

public interface Hasher {

    String hashing(String target);

    boolean matches(String rawTarget, String hashedTarget);
}
