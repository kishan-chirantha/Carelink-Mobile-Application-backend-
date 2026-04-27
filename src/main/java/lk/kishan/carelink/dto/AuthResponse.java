package lk.kishan.carelink.dto;

public class AuthResponse {
    private String token;
    private Long customerId;
    private String email;
    private String role;

    public AuthResponse(String token, Long customerId, String email, String role) {
        this.token = token;
        this.customerId = customerId;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}