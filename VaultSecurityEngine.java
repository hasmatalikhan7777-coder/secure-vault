import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class VaultSecurityEngine {
    private static final String ALGORITHM = "AES";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private byte[] secretKey;

    // Generates a random salt for new vaults
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    // Initialize the engine by deriving the AES key from the user's password
    public VaultSecurityEngine(char[] masterPassword, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        this.secretKey = factory.generateSecret(spec).getEncoded();
        Arrays.fill(masterPassword, '\0'); // Wipe password from RAM instantly
    }

    public String encryptData(char[] rawData) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(this.secretKey, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        
        byte[] dataBytes = new String(rawData).getBytes();
        byte[] encrypted = cipher.doFinal(dataBytes);
        Arrays.fill(dataBytes, (byte) 0); 
        
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public char[] decryptData(String encryptedBase64) throws VaultTamperedException, AuthenticationFailedException {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(this.secretKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            byte[] decrypted = cipher.doFinal(decoded);
            
            char[] result = new String(decrypted).toCharArray();
            Arrays.fill(decrypted, (byte) 0); 
            return result;
        } catch (javax.crypto.BadPaddingException e) {
            throw new AuthenticationFailedException("Incorrect Master Password.");
        } catch (IllegalArgumentException e) {
            throw new VaultTamperedException("CSV File altered.");
        } catch (Exception e) {
            throw new RuntimeException("System Error", e);
        }
    }
}