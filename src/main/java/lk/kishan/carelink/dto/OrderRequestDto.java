package lk.kishan.carelink.dto;

import lombok.Data;

@Data
public class OrderRequestDto {
    private Long customerId;
    private Long pharmacyId;
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;
    private String paymentMethod;
    private Double itemsTotal;
    private Double deliveryFee;
    private Double totalAmount;
}