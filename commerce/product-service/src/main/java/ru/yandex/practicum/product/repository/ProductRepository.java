package ru.yandex.practicum.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.product.entity.Product;

import java.util.Collection;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Collection<Product> findByCategoryId(Long categoryId);

    Collection<Product> findByNameContainingIgnoreCase(String query);
}
