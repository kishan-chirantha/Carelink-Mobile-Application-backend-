package lk.kishan.carelink.controller;

import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.repository.PharmacyRepository;
import lk.kishan.carelink.service.PharmacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacies")
@CrossOrigin(origins = "*")
public class PharmacyController {

    @Autowired
    private PharmacyService pharmacyService;


    @PostMapping("/register")
    public String register(@RequestBody Pharmacy pharmacy) {
        return pharmacyService.registerPharmacy(pharmacy);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Pharmacy>> getNearbyPharmacies(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10.0") Double radius) {

        List<Pharmacy> pharmacies = pharmacyService.getNearbyPharmacies(lat, lng, radius);
        return ResponseEntity.ok(pharmacies);
    }

    @GetMapping
    public List<Pharmacy> getAllPharmacies() {
        return pharmacyService.getAllPharmacies();
    }

}