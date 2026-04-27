package lk.kishan.carelink.service;

import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.model.Role;
import lk.kishan.carelink.repository.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PharmacyService {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    public String registerPharmacy(Pharmacy pharmacy) {

        if (pharmacyRepository.existsByEmail(pharmacy.getEmail())) {
            return "Error: Email already exists!";
        }

        pharmacy.setRole(Role.PHARMACY);

        pharmacyRepository.save(pharmacy);
        return "Pharmacy Registration Successful!";
    }

    public List<Pharmacy> getNearbyPharmacies(Double lat, Double lng, Double radius) {
        return pharmacyRepository.findPharmaciesWithinRadius(lat, lng, radius);
    }

    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepository.findAll();
    }
}