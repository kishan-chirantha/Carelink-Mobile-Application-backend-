package lk.kishan.carelink.controller;

import lk.kishan.carelink.dto.AuthRequest;
import lk.kishan.carelink.dto.AuthResponse;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.repository.CustomerRepository;
import lk.kishan.carelink.security.JwtUtil;
import lk.kishan.carelink.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody lk.kishan.carelink.dto.GoogleAuthRequest request) {
        try {
            Customer customer = customerService.getCustomerByEmail(request.getEmail());

            if (customer == null) {
                customer = new Customer();
                customer.setEmail(request.getEmail());
                customer.setName(request.getName());
                customer.setPassword("GOOGLE_OAUTH_DUMMY_PASSWORD_123!");

                customerService.registerCustomer(customer);

                customer = customerService.getCustomerByEmail(request.getEmail());
            }

            String token = jwtUtil.generateToken(customer.getEmail());

            return ResponseEntity.ok(new AuthResponse(token, customer.getId(), customer.getEmail(), "CUSTOMER"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Google Login Processing Failed!");
        }
    }

    @PostMapping("/register")
    public String register(@RequestBody Customer customer) {
        return customerService.registerCustomer(customer);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );

            String token = jwtUtil.generateToken(authRequest.getEmail());
            Customer customer = customerService.getCustomerByEmail(authRequest.getEmail());

            return ResponseEntity.ok(new AuthResponse(token, customer.getId(), customer.getEmail(), "CUSTOMER"));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Email or Password!");
        }
    }

    @GetMapping("/{email}")
    public Customer getCustomer(@PathVariable String email) {
        return customerService.getCustomerByEmail(email);
    }

    @PutMapping("/{id}/update-phone")
    public ResponseEntity<?> updatePhoneNumber(@PathVariable Long id, @RequestParam String phone) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(id);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                customer.setMobile(phone);
                customerRepository.save(customer);
                return ResponseEntity.ok().body("Phone number updated successfully");
            } else {
                return ResponseEntity.badRequest().body("Customer not found");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{customerId}/update-profile")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long customerId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            if (name != null && !name.isEmpty()) customer.setName(name);
            if (mobile != null && !mobile.isEmpty()) customer.setMobile(mobile);

            if (image != null && !image.isEmpty()) {

                String folderPath = "uploads/profile_images/" + customerId + "/";
                File directory = new File(folderPath);

                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String oldImage = customer.getProfileImage();
                if (oldImage != null) {
                    File oldFile = new File(folderPath + oldImage);
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }

                String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                Path filePath = Paths.get(folderPath + fileName);
                Files.write(filePath, image.getBytes());

                customer.setProfileImage(fileName);
            }

            customerRepository.save(customer);
            return ResponseEntity.ok("Profile updated successfully!");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating profile: " + e.getMessage());
        }
    }
}