package lk.kishan.carelink.controller;

import lk.kishan.carelink.dto.AuthRequest;
import lk.kishan.carelink.dto.AuthResponse;
import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.repository.PharmacyRepository;
import lk.kishan.carelink.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/pharmacy-login")
    public ResponseEntity<?> staffLogin(@RequestBody AuthRequest request) {
        try {
            Pharmacy pharmacy = pharmacyRepository.findByEmail(request.getEmail());

            if (pharmacy == null) {
                return ResponseEntity.status(401).body("Invalid Email!");
            }

            if (!request.getPassword().equals(pharmacy.getPassword())) {
                return ResponseEntity.status(401).body("Invalid Password!");
            }

            if (pharmacy.getIsActive() != null && !pharmacy.getIsActive()) {
                return ResponseEntity.status(403).body("Account is Disabled by Admin!");
            }

            String token = jwtUtil.generateToken(pharmacy.getEmail());
            String role = pharmacy.getRole().name();

            return ResponseEntity.ok(new AuthResponse(token, pharmacy.getId(), pharmacy.getEmail(), role));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Login Failed: " + e.getMessage());
        }
    }
}