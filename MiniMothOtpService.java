import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MiniMothOtpService {
    private static final String BASE_URL = "https://api.minimoth.dev";
    private final String apiKey;

    public MiniMothOtpService(String apiKey) {
        this.apiKey = apiKey;
    }

    public static class OtpResult {
        public final boolean success;
        public final String message;
        public final String otpId;

        public OtpResult(boolean success, String message, String otpId) {
            this.success = success;
            this.message = message;
            this.otpId = otpId;
        }
    }

    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replaceAll("[\\s-]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.matches("^\\d{10}$")) {
            return "91" + cleaned;
        }
        return cleaned;
    }

    public OtpResult sendOtp(String rawPhone) {
        String phone = normalizePhone(rawPhone);
        if (phone.isEmpty()) {
            return new OtpResult(false, "Phone number is required.", null);
        }

        try {
            String jsonPayload = "{\"phone\":\"" + escapeJson(phone) + "\"}";
            ApiResponse response = makeRequest("/v1/otp/send", "POST", jsonPayload);

            if (response.statusCode >= 200 && response.statusCode < 300) {
                String otpId = extractJsonString(response.body, "otp_id");
                return new OtpResult(true, "OTP sent successfully.", otpId);
            } else {
                String errorMsg = extractJsonString(response.body, "error");
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = extractJsonString(response.body, "code");
                }
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "HTTP " + response.statusCode;
                }
                return new OtpResult(false, errorMsg, null);
            }
        } catch (Exception e) {
            return new OtpResult(false, "Network error: " + e.getMessage(), null);
        }
    }

    public OtpResult verifyOtp(String rawPhone, String otpCode) {
        String phone = normalizePhone(rawPhone);
        if (phone.isEmpty()) {
            return new OtpResult(false, "Phone number is required.", null);
        }
        if (otpCode == null || !otpCode.trim().matches("^\\d{6}$")) {
            return new OtpResult(false, "OTP must be a 6-digit number.", null);
        }

        try {
            String jsonPayload = "{\"phone\":\"" + escapeJson(phone) + "\",\"code\":\"" + escapeJson(otpCode.trim()) + "\"}";
            ApiResponse response = makeRequest("/v1/otp/verify", "POST", jsonPayload);

            if (response.statusCode >= 200 && response.statusCode < 300) {
                return new OtpResult(true, "OTP verified successfully.", null);
            } else {
                String errorMsg = extractJsonString(response.body, "error");
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = extractJsonString(response.body, "code");
                }
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "Verification failed (HTTP " + response.statusCode + ")";
                }
                return new OtpResult(false, errorMsg, null);
            }
        } catch (Exception e) {
            return new OtpResult(false, "Network error: " + e.getMessage(), null);
        }
    }

    private static class ApiResponse {
        final int statusCode;
        final String body;

        ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private ApiResponse makeRequest(String path, String method, String jsonBody) throws Exception {
        URL url = URI.create(BASE_URL + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Api-Key", apiKey);
        conn.setRequestProperty("User-Agent", "SecureVault/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int status = conn.getResponseCode();
        InputStreamReader isr;
        if (status >= 200 && status < 400) {
            isr = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
        } else {
            java.io.InputStream errorStream = conn.getErrorStream();
            isr = new InputStreamReader(errorStream != null ? errorStream : conn.getInputStream(), StandardCharsets.UTF_8);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        return new ApiResponse(status, response.toString());
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
