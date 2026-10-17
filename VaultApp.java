import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class VaultApp {
    private JFrame frame;
    private JPanel cards;
    private DefaultTableModel tableModel;
    private VaultStorageManager storageManager;
    private ArrayList<SecureRecord> memoryVault;

    public VaultApp() {
        storageManager = new VaultStorageManager();
        memoryVault = new ArrayList<>();

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception ignored) {}

        frame = new JFrame("Offline Secure Data Vault");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(620, 480);
        frame.setLocationRelativeTo(null);

        cards = new JPanel(new CardLayout());
        cards.add(buildLoginPanel(), "Login");
        cards.add(buildVaultPanel(), "Vault");

        frame.add(cards);
        frame.setVisible(true);
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(43, 43, 43)); 

        JLabel title = new JLabel(storageManager.vaultExists() ? "UNLOCK VAULT" : "CREATE NEW VAULT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel mobileLabel = new JLabel("Mobile Number (with +country code):");
        mobileLabel.setForeground(Color.LIGHT_GRAY);
        JTextField phoneField = new JTextField(15);
        phoneField.setToolTipText("e.g. +919876543210");

        JButton sendOtpBtn = new JButton("Send OTP");

        JLabel otpLabel = new JLabel("Enter 6-Digit OTP:");
        otpLabel.setForeground(Color.LIGHT_GRAY);
        JTextField otpField = new JTextField(10);
        JButton verifyOtpBtn = new JButton("Verify OTP");
        JLabel otpStatusLabel = new JLabel("OTP Verification Required");
        otpStatusLabel.setForeground(new Color(255, 140, 0));

        JLabel keyLabel = new JLabel("Master Password:");
        keyLabel.setForeground(Color.LIGHT_GRAY);
        JPasswordField masterKeyField = new JPasswordField(15);
        JButton unlockBtn = new JButton("Access Vault");
        unlockBtn.setEnabled(false); // Unlocks only after OTP verification

        MiniMothOtpService otpService = new MiniMothOtpService("mm_live_618a7cf4dd4094325b51406969bbeedff539e25dcbb1c2efc79d2cc78dd058444a2e3a968bd57a4ef0ff36cdf43e30ca");

        sendOtpBtn.addActionListener(e -> {
            String phone = phoneField.getText().trim();
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter your mobile number first.");
                return;
            }
            sendOtpBtn.setEnabled(false);
            sendOtpBtn.setText("Sending...");
            new SwingWorker<MiniMothOtpService.OtpResult, Void>() {
                @Override
                protected MiniMothOtpService.OtpResult doInBackground() {
                    return otpService.sendOtp(phone);
                }
                @Override
                protected void done() {
                    try {
                        MiniMothOtpService.OtpResult result = get();
                        if (result.success) {
                            otpStatusLabel.setText("OTP sent! Please check your SMS/WhatsApp.");
                            otpStatusLabel.setForeground(new Color(100, 220, 100));
                            JOptionPane.showMessageDialog(frame, "OTP sent successfully! Please check SMS/WhatsApp.");
                        } else {
                            otpStatusLabel.setText("Failed to send OTP: " + result.message);
                            otpStatusLabel.setForeground(new Color(255, 80, 80));
                            JOptionPane.showMessageDialog(frame, "Error: " + result.message, "Send OTP Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        otpStatusLabel.setText("Error sending OTP.");
                        otpStatusLabel.setForeground(new Color(255, 80, 80));
                    } finally {
                        sendOtpBtn.setEnabled(true);
                        sendOtpBtn.setText("Resend OTP");
                    }
                }
            }.execute();
        });

        verifyOtpBtn.addActionListener(e -> {
            String phone = phoneField.getText().trim();
            String code = otpField.getText().trim();
            if (phone.isEmpty() || code.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter both mobile number and OTP code.");
                return;
            }
            verifyOtpBtn.setEnabled(false);
            verifyOtpBtn.setText("Verifying...");
            new SwingWorker<MiniMothOtpService.OtpResult, Void>() {
                @Override
                protected MiniMothOtpService.OtpResult doInBackground() {
                    return otpService.verifyOtp(phone, code);
                }
                @Override
                protected void done() {
                    try {
                        MiniMothOtpService.OtpResult result = get();
                        if (result.success) {
                            otpStatusLabel.setText("Phone Verified Successfully!");
                            otpStatusLabel.setForeground(new Color(60, 220, 60));
                            unlockBtn.setEnabled(true);
                            verifyOtpBtn.setEnabled(false);
                            sendOtpBtn.setEnabled(false);
                            phoneField.setEditable(false);
                            otpField.setEditable(false);
                            JOptionPane.showMessageDialog(frame, "Mobile verified! Enter master password to unlock.");
                        } else {
                            otpStatusLabel.setText("Verification Failed: " + result.message);
                            otpStatusLabel.setForeground(new Color(255, 80, 80));
                            JOptionPane.showMessageDialog(frame, "Verification Failed: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
                            verifyOtpBtn.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        otpStatusLabel.setText("Error verifying OTP.");
                        otpStatusLabel.setForeground(new Color(255, 80, 80));
                        verifyOtpBtn.setEnabled(true);
                    } finally {
                        verifyOtpBtn.setText("Verify OTP");
                    }
                }
            }.execute();
        });

        unlockBtn.addActionListener(e -> {
            try {
                char[] password = masterKeyField.getPassword();
                storageManager.initializeVault(password);
                memoryVault = storageManager.loadVaultData();
                refreshTable();
                ((CardLayout) cards.getLayout()).show(cards, "Vault");
            } catch (VaultTamperedException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "CRITICAL ALERT", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Close app if Dead Man's Switch triggers
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "System Error: " + ex.getMessage());
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(mobileLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(phoneField, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(sendOtpBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(otpLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        panel.add(otpField, gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(verifyOtpBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(otpStatusLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(keyLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1;
        panel.add(masterKeyField, gbc);
        gbc.gridx = 1; gbc.gridy = 7;
        panel.add(unlockBtn, gbc);

        return panel;
    }

    private JPanel buildVaultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel toolbar = new JPanel();
        JTextField accountField = new JTextField(10);
        JTextField userField = new JTextField(10);
        JPasswordField passField = new JPasswordField(10);
        JButton addBtn = new JButton("Add Record");

        toolbar.add(new JLabel("Account:")); toolbar.add(accountField);
        toolbar.add(new JLabel("User:")); toolbar.add(userField);
        toolbar.add(new JLabel("Secret:")); toolbar.add(passField);
        toolbar.add(addBtn);

        String[] columns = {"Account", "Username", "Secure Data"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        
        // Custom renderer to visually hide passwords in the table
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value != null ? "********" : "");
            }
        });

        addBtn.addActionListener(e -> {
            SecureRecord record = new SecureRecord(accountField.getText(), userField.getText(), passField.getPassword());
            memoryVault.add(record);
            storageManager.saveVaultData(memoryVault);
            refreshTable();
            
            accountField.setText(""); userField.setText(""); passField.setText("");
        });

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (SecureRecord record : memoryVault) {
            tableModel.addRow(new Object[]{record.getAccountName(), record.getUsername(), new String(record.getSecureData())});
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VaultApp::new);
    }
}