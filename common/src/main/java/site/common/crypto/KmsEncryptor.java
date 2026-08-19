package site.common.crypto;

public interface KmsEncryptor {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
