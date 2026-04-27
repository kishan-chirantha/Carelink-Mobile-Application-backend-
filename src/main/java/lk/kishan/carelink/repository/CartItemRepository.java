package lk.kishan.carelink.repository;

import lk.kishan.carelink.model.Cart;
import lk.kishan.carelink.model.CartItem;
import lk.kishan.carelink.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    CartItem findByCartAndProduct(Cart cart, Product product);

    List<CartItem> findByCart_Customer_Id(Long customerId);
}
