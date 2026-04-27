package lk.kishan.carelink.controller;

import lk.kishan.carelink.model.Prescription;
import lk.kishan.carelink.model.OrderStatus;
import lk.kishan.carelink.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @PutMapping("/{id}/price")
    public ResponseEntity<?> updatePrice(@PathVariable Long id, @RequestParam Double price) {
        Prescription p = prescriptionRepository.findById(id).orElse(null);
        if (p != null) {
            p.setPrice(price);
            prescriptionRepository.save(p);
            return ResponseEntity.ok("Price updated successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Prescription p = prescriptionRepository.findById(id).orElse(null);
        if (p != null) {
            p.setStatus(OrderStatus.valueOf(status));
            prescriptionRepository.save(p);
            return ResponseEntity.ok("Status updated successfully");
        }
        return ResponseEntity.notFound().build();
    }
}