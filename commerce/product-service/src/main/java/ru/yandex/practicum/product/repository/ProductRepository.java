package ru.yandex.practicum.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.product.entity.Product;

import java.util.Collection;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Collection<Product> findByActiveTrue();

    Collection<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    Collection<Product> findByNameContainingIgnoreCaseAndActiveTrue(String query);
}
