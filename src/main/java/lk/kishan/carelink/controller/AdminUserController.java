package lk.kishan.carelink.controller;

import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.model.Role;
import lk.kishan.carelink.repository.PharmacyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {

    private final PharmacyRepository pharmacyRepository;

    public AdminUserController(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    @GetMapping("/admins")
    public List<Pharmacy> getAllAdmins() {
        return pharmacyRepository.findByRole(Role.ADMIN);
    }

    @PostMapping("/admins")
    public Pharmacy createAdmin(@RequestBody Pharmacy admin) {
        admin.setRole(Role.ADMIN);
        return pharmacyRepository.save(admin);
    }
}