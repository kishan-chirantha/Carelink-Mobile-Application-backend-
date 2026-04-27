package lk.kishan.carelink.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pharmacy")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String pharmacyName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    private Double latitude;

    private Double longitude;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private boolean isActive = true;

    public Boolean getIsActive() { return isActive; }
}