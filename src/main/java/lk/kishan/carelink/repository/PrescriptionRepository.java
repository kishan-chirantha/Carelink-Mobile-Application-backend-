package lk.kishan.carelink.repository;

import lk.kishan.carelink.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByCart_Customer_IdAndOrderIsNull(Long customerId);
}