package lk.kishan.carelink.service;

import lk.kishan.carelink.model.CartItem;
import lk.kishan.carelink.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    public void updateQuantities(Map<Long, Integer> quantities) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long cartItemId = entry.getKey();
            Integer newQuantity = entry.getValue();

            CartItem cartItem = cartItemRepository.findById(cartItemId).orElse(null);
            if (cartItem != null) {
                cartItem.setQuantity(newQuantity);
                cartItemRepository.save(cartItem);
            }
        }
    }
}