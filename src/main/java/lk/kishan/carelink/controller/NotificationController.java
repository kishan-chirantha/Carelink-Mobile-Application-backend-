package lk.kishan.carelink.controller;

import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.model.Notification;
import lk.kishan.carelink.repository.CustomerRepository;
import lk.kishan.carelink.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/notifications/customer/{customerId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long customerId) {
        List<Notification> notifications = notificationRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/customers/{customerId}/fcm-token")
    public ResponseEntity<?> updateFcmToken(@PathVariable Long customerId, @RequestParam String token) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setFcmToken(token);
            customerRepository.save(customer);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}