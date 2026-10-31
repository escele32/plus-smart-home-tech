package ru.yandex.practicum.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "customer_name", nullable = false)
    String customerName;

    @Column(name = "customer_email", nullable = false)
    String customerEmail;

    @Column(name = "status", nullable = false)
    String status;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    BigDecimal totalPrice;

    @Column(name = "status_details")
    String statusDetails;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @OneToMany(fetch = FetchType.EAGER)
    @Builder.Default
    List<OrderItem> items = new ArrayList<>();
}
