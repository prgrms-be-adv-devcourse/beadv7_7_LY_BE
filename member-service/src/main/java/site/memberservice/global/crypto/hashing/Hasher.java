package site.memberservice.global.crypto.hashing;

public interface Hasher {

    String hash(String target);

    boolean match(String rawTarget, String hashedTarget);
}
