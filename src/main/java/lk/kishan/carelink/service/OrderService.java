package lk.kishan.carelink.service;

import jakarta.transaction.Transactional;
import lk.kishan.carelink.dto.OrderRequestDto;
import lk.kishan.carelink.model.*;
import lk.kishan.carelink.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PharmacyRepository pharmacyRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired
    private FCMService fcmService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Transactional
    public Order placeOrder(OrderRequestDto request) {

        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow();
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId()).orElseThrow();

        Order order = new Order();

        String generatedTrackingId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        order.setTrackingId(generatedTrackingId);
        order.setCustomer(customer);
        order.setPharmacy(pharmacy);
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryLat(request.getDeliveryLat());
        order.setDeliveryLng(request.getDeliveryLng());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setItemsTotal(request.getItemsTotal());
        order.setDeliveryFee(request.getDeliveryFee());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        List<CartItem> cartItems = cartItemRepository.findByCart_Customer_Id(customer.getId());

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getProduct().getPrice());

            order.getOrderItems().add(orderItem);
        }

        List<Prescription> cartPrescriptions = prescriptionRepository.findByCart_Customer_IdAndOrderIsNull(customer.getId());

        for (Prescription prescription : cartPrescriptions) {
            prescription.setOrder(order);
            prescription.setCart(null);
        }

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }

}