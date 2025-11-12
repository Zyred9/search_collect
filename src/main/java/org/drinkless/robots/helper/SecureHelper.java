package org.drinkless.robots.helper;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Slf4j
public class SecureHelper {

    public static PrivateKey privateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static String encrypt(String plainText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static PublicKey publicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static String decrypt(String encryptedText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }


    public static void main(String[] args) throws Exception {
        String val = "7612958407:AAGlZ1hsyQbJEyEO7vMVfR7n46qcxFuJeR4";
        String key = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAJhRqqNZsYq0FtAQjFPbhZxNhlzIpVvvgysIz/yUX/nw83DBmiYpuLbRxA/kp8pwDWsVMZcNbIVcgt4XYMJzlthKQ8t5ufbCE+DR2czAmOyWzQpKYbXyNVrCj3CyIH9vhopaudcFqwpL6Om4JUSuBcy2MarEBydCIsXGbKT4EkzpAgMBAAECgYAi7VETQXEyZL65aZ3/6Z2+3FEDzBeM2ARMFSp6GLzUR4HO+HnkJ9t9AmcynMQz6ZNOG29CW5fnkbHJgvNTS2WKahjPJmLU9UcLp3I15nI/KPgw1XNv2+MttqjIHdgErlhn1qwMUB0JQwxP6Kl839oTaE9qKbykdGDWtikUvs/RrQJBAKpJqKN1Id3M7DlOKjJi4Tu+2KhLdSp1/hRkM2SfTuPo53+ECTTzW1NR5D1fUDzGNR1cojNg06VuvIcxgiAIRwUCQQDk/KhHTTp0EziKbPURHX/Icv+cfHv2n5O05vHyFDYJitoNmQrhGog13B74B71e1qjk1VCjX+mtve+vjklKMMuVAkByMSrxUh7/yeevFged+kjn87b+RHuxmaZkrjz4gQw6MXjsPfKem4LmgMf5j+0SlCgSJIhww8Gp8nRihISqTmKxAkEA2wsBYitzBgQ46tmtV/DzAmlXMHc4EcO2hK8CtEI3KsujKKzEZm5965+kFDk7IhSPPU78szuVijiNpk6itxCUPQJBAI74JfrTrJZQW69opOuFTtS8BBJIih3AHlE9pkBc+jwv8NTUATibM8v/hCpxi/1dK+gfOboQO1Vgh3ssvylSOcE=";

        PrivateKey privateKey = privateKey(key);
        String encrypt = encrypt(val, privateKey);
        System.out.println(encrypt);

    }
}
