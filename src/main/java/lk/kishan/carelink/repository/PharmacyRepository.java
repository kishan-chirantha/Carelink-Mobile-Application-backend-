package lk.kishan.carelink.repository;

import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    List<Pharmacy> findByRole(Role role);

    boolean existsByEmail(String email);

    Pharmacy findByEmail(String email);

    @Query(value = "SELECT *, (6371 * acos(cos(radians(:userLat)) * cos(radians(latitude)) * " +
            "cos(radians(longitude) - radians(:userLng)) + " +
            "sin(radians(:userLat)) * sin(radians(latitude)))) AS distance " +
            "FROM pharmacy " +
            "WHERE is_active = true " +
            "HAVING distance <= :radius " +
            "ORDER BY distance ASC",
            nativeQuery = true)
    List<Pharmacy> findPharmaciesWithinRadius(
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("radius") Double radius
    );
}