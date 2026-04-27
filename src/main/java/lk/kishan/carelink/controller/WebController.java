package lk.kishan.carelink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/pharmacy-login")
    public String showLoginPage() {
        return "index";
    }

    @GetMapping("/pharmacy-signup")
    public String showSignupPage() {
        return "signup";
    }

    @GetMapping("/admin-dashboard")
    public String showAdminDashboard() {
        return "admin_dashboard";
    }

    @GetMapping("/pharmacy-dashboard")
    public String showPharmacyDashboard() {
        return "pharmacy_dashboard";
    }
}