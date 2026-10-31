package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "inventories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "product_id", unique = true)
    Long productId;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "reservedQuantity", nullable = false)
    Integer reservedQuantity;

    @Version
    Long version;

    @Transient
    Integer availableQuantity;

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    @SuppressWarnings("unused")
    private void setAvailableQuantity(Integer availableQuantity) {
    }
}
