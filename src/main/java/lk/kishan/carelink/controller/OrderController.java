package lk.kishan.carelink.controller;

import lk.kishan.carelink.dto.OrderRequestDto;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.model.Notification;
import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.model.OrderStatus;
import lk.kishan.carelink.repository.NotificationRepository;
import lk.kishan.carelink.repository.OrderRepository;
import lk.kishan.carelink.service.FCMService;
import lk.kishan.carelink.service.InvoiceService;
import lk.kishan.carelink.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin("*")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FCMService fcmService;
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private InvoiceService invoiceService;


    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDto request) {
        try {
            Order newOrder = orderService.placeOrder(request);

            return ResponseEntity.ok(newOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Order placement failed: " + e.getMessage());
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getCustomerOrders(@PathVariable Long customerId) {
        try {
            List<Order> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            sendNotificationToCustomer(order.getCustomer(),
                    "Order Cancelled",
                    "Your order #" + order.getTrackingId() + " has been cancelled.",
                    "ORDER");

            return ResponseEntity.ok("Order Cancelled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(
            @PathVariable Long orderId,
            @RequestParam double finalAmount,
            @RequestParam String paymentStatus) {

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            order.setStatus(OrderStatus.valueOf(paymentStatus.toUpperCase()));

            order.setTotalAmount(finalAmount);

            orderRepository.save(order);

            sendNotificationToCustomer(order.getCustomer(),
                    "Payment Successful",
                    "We have received your payment for order #" + order.getTrackingId() + ". Pharmacy will dispatch it soon.",
                    "ORDER");

            return ResponseEntity.ok("Order Confirmed");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid Order Status: " + paymentStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error confirming order: " + e.getMessage());
        }
    }

    private void sendNotificationToCustomer(Customer customer, String title, String body, String type) {
        if (customer == null) return;

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setRead(false);
        notification.setTimestamp(LocalDateTime.now());
        notification.setCustomer(customer);

        notificationRepository.save(notification);

        if (customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
            fcmService.sendPushNotification(customer.getFcmToken(), title, body);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus newStatus) {

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            order.setStatus(newStatus);
            orderRepository.save(order);

            String title = "";
            String body = "";

            switch (newStatus) {
                case OUT_FOR_DELIVERY:
                    title = "Order Out for Deliver!";
                    body = "Your order #" + order.getTrackingId() + " is on the way to your delivery address. Please be ready.";
                    break;
                case DELIVERED:
                    title = "Order Delivered!";
                    body = "Your order #" + order.getTrackingId() + " has been successfully delivered. Stay healthy!";
                    break;
                case CANCELLED:
                    title = "Order Cancelled";
                    body = "Unfortunately, your order #" + order.getTrackingId() + " has been cancelled by the pharmacy.";
                    break;
                default:
                    title = "Order Update!";
                    body = "Your order #" + order.getTrackingId() + " status has been updated to: " + newStatus;
                    break;
            }

            sendNotificationToCustomer(order.getCustomer(), title, body, "ORDER");

            return ResponseEntity.ok("Order status updated to " + newStatus + " and notification sent!");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId) {
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(orderId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Invoice_" + orderId + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}