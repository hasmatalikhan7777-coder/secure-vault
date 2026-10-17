class VaultTamperedException extends Exception {
    public VaultTamperedException(String message) { super(message); }
}

class AuthenticationFailedException extends Exception {
    public AuthenticationFailedException(String message) { super(message); }
}