import java.util.Arrays;

public class SecureRecord {
    private String accountName;
    private String username;
    private char[] secureData; 

    public SecureRecord(String accountName, String username, char[] secureData) {
        this.accountName = accountName;
        this.username = username;
        this.secureData = Arrays.copyOf(secureData, secureData.length);
    }

    public String getAccountName() { return accountName; }
    public String getUsername() { return username; }
    public char[] getSecureData() { return secureData; }

    public void clearMemory() {
        if (secureData != null) {
            Arrays.fill(secureData, '\0');
        }
    }
}