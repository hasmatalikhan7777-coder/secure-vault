import java.io.*;
import java.util.ArrayList;

public class VaultStorageManager {
    private static final String DATA_FILE = "vault_data.csv";
    private static final String SALT_FILE = "vault_config.dat";
    private VaultSecurityEngine securityEngine;

    // Checks if a vault already exists
    public boolean vaultExists() {
        return new File(SALT_FILE).exists();
    }

    // Loads or creates the salt, then initializes the security engine
    public void initializeVault(char[] masterPassword) throws Exception {
        byte[] salt;
        File saltFile = new File(SALT_FILE);
        
        if (saltFile.exists()) {
            salt = new byte[16];
            try (FileInputStream fis = new FileInputStream(saltFile)) { fis.read(salt); }
        } else {
            salt = VaultSecurityEngine.generateSalt();
            try (FileOutputStream fos = new FileOutputStream(saltFile)) { fos.write(salt); }
        }
        this.securityEngine = new VaultSecurityEngine(masterPassword, salt);
    }

    public ArrayList<SecureRecord> loadVaultData() throws VaultTamperedException {
        ArrayList<SecureRecord> records = new ArrayList<>();
        File file = new File(DATA_FILE);
        if (!file.exists()) return records;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3) throw new VaultTamperedException("Corrupted row.");
                
                char[] decryptedData = securityEngine.decryptData(parts[2]);
                records.add(new SecureRecord(parts[0], parts[1], decryptedData));
            }
        } catch (VaultTamperedException | AuthenticationFailedException | IllegalArgumentException e) {
            executeSelfDestruct(file);
            throw new VaultTamperedException("BREACH DETECTED: Data tampered with or incorrect password. Vault has been securely wiped.");
        } catch (Exception e) {
            System.out.println("Read error: " + e.getMessage());
        }
        return records;
    }

    public void saveVaultData(ArrayList<SecureRecord> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (SecureRecord record : records) {
                String encryptedData = securityEngine.encryptData(record.getSecureData());
                writer.println(record.getAccountName() + "," + record.getUsername() + "," + encryptedData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void executeSelfDestruct(File file) {
        if (file.exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
                raf.write(new byte[(int) raf.length()]);
            } catch (IOException ignored) {}
            file.delete();
            new File(SALT_FILE).delete(); // Also delete the salt to completely reset
        }
    }
}