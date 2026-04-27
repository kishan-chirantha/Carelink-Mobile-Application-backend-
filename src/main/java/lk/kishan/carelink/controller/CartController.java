package lk.kishan.carelink.controller;

import lk.kishan.carelink.model.*;
import lk.kishan.carelink.repository.*;
import lk.kishan.carelink.service.CartService;
import lk.kishan.carelink.service.ImageUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private PrescriptionRepository prescriptionRepository;
    @Autowired
    private ImageUploadService imageUploadService;
    @Autowired
    private CartService cartService;

    @PostMapping("/add-item")
    public ResponseEntity<?> addItemToCart(@RequestParam Long customerId,
                                           @RequestParam Long productId,
                                           @RequestParam int quantity) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Customer not found!");
            }
            Customer customer = customerOpt.get();

            Cart cart = customer.getCart();
            if (cart == null) {
                cart = new Cart();
                cart.setCustomer(customer);
                cart = cartRepository.save(cart);
            }

            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) return ResponseEntity.badRequest().body("Product not found!");

            Product selectedProduct = productOpt.get();

            CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, selectedProduct);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                cartItemRepository.save(existingItem);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(selectedProduct);
                newItem.setQuantity(quantity);
                cartItemRepository.save(newItem);
            }

            return ResponseEntity.ok("Item added to cart successfully!");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/add-prescription")
    public ResponseEntity<?> addPrescriptionToCart(@RequestParam Long customerId,
                                                   @RequestParam("file") MultipartFile file) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Customer not found!");
            }
            Customer customer = customerOpt.get();

            Cart cart = customer.getCart();
            if (cart == null) {
                cart = new Cart();
                cart.setCustomer(customer);
                cart = cartRepository.save(cart);
            }

            String imageUrl = imageUploadService.savePrescriptionImage(file);

            Prescription prescription = new Prescription(imageUrl);
            prescription.setCart(cart);
            prescriptionRepository.save(prescription);

            return ResponseEntity.ok("Prescription added to cart successfully!");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error uploading prescription: " + e.getMessage());
        }
    }

    @GetMapping("/get/{customerId}")
    public ResponseEntity<?> getCartDetails(@PathVariable Long customerId) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Customer not found!");
            }

            Cart cart = customerOpt.get().getCart();
            if (cart == null) {
                return ResponseEntity.ok(new Cart());
            }

            return ResponseEntity.ok(cart);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching cart: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove-item/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId) {
        try {
            if (!cartItemRepository.existsById(cartItemId)) {
                return ResponseEntity.badRequest().body("Product not found!");
            }
            cartItemRepository.deleteById(cartItemId);
            return ResponseEntity.ok("Item removed successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error removing item: " + e.getMessage());
        }
    }

    @PutMapping("/update-quantities")
    public ResponseEntity<String> updateCartQuantities(@RequestBody Map<Long, Integer> quantities) {
        cartService.updateQuantities(quantities);
        return ResponseEntity.ok("Cart quantities updated successfully!");
    }

}