package ru.yandex.practicum.product.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductServiceImpl implements ProductService {
    ProductRepository productRepository;
    CategoryRepository categoryRepository;

    private Category validateCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> {
            log.warn("Категории с id {} не существует", categoryId);
            return new NotFoundException(format("Категории с id %d не существует", categoryId));
        });
    }

    private Product validateProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(() -> {
            log.warn("Продукта с id {} не существует", productId);
            return new NotFoundException(format("Продукта с id %d не существует", productId));
        });
    }

    @Override
    @Transactional
    public ProductDto create(CreateProductRequest request) {
        log.debug("Добавление продукта {}", request);
        Category category = validateCategory(request.categoryId());
        Product product = productRepository.save(Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(category)
                .imageUrl(request.imageUrl())
                .build()
        );
        log.info("Создан продукт с id {}", product.getId());
        return ProductMapper.toDto(product);
    }

    @Override
    @Transactional
    public ProductDto update(Long productId, UpdateProductRequest request) {
        log.debug("Обновление данных продукта с id {}", productId);
        Product product = validateProduct(productId);
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.categoryId() != null) {
            product.setCategory(validateCategory(request.categoryId()));
        }
        if (!request.imageUrl().isEmpty()) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        log.info("Данные продукта с id {} обновлены", product.getId());
        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public List<ProductDto> getAll() {
        List<Product> products = productRepository.findByActiveTrue().stream().toList();
        log.info("Получение списка продуктов");
        return products.stream().map(ProductMapper::toDto).toList();
    }

    @Override
    public ProductDto getById(Long productId) {
        Product product = validateProduct(productId);
        log.info("Получерие продукта с id {}", product.getId());
        return ProductMapper.toDto(product);
    }

    @Override
    public List<ProductDto> getProductsCategoryId(Long categoryId) {
        Category category = validateCategory(categoryId);
        List<Product> products = productRepository.findByCategoryIdAndActiveTrue(category.getId()).stream().toList();
        log.info("Получение списка продуктов по категории с id {}", category.getId());
        return products.stream().map(ProductMapper::toDto).toList();
    }

    @Override
    public List<ProductDto> getSearch(String query) {
        log.debug("Получение списка продуктов по запросу {}", query);
        String searchQuery = (query == null || query.trim().isEmpty()) ? "" : query.trim();
        if (searchQuery.isEmpty()) {
            log.info("По данному запросу {} список продуктов пуст", searchQuery);
            return Collections.emptyList();
        }
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(searchQuery)
                .stream().toList();
        log.info("По данному запросу {} получен список продуктов", searchQuery);
        return products.stream().map(ProductMapper::toDto).toList();
    }
}
