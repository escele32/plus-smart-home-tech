package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;
import ru.yandex.practicum.product.util.ApiPath;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API + ApiPath.PRODUCTS)
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
    ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("Запрос на добавление продукта");
        return service.create(request);
    }

    @PatchMapping(ApiPath.ID)
    @ResponseStatus(HttpStatus.OK)
    public ProductDto updateProduct(
            @PathVariable("id") Long productId,
            @Valid @RequestBody UpdateProductRequest request
            ) {
        log.info("Запрос на обновление данных продукта с id {}", productId);
        return service.update(productId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductDto> findAll() {
        log.info("Запрос на получение списка продуктов");
        return service.getAll();
    }

    @GetMapping(ApiPath.ID)
    @ResponseStatus(HttpStatus.OK)
    public ProductDto findByProductId(@PathVariable("id") Long productId) {
        log.info("Запрос на получение продукта по id{}", productId);
        return service.getById(productId);
    }

    @GetMapping(ApiPath.CATEGORY + ApiPath.CATEGORY_ID)
    @ResponseStatus(HttpStatus.OK)
    public List<ProductDto> findProductsByCategoryId(@PathVariable Long categoryId) {
        log.info("Запрос на получение списка продуктов по категории с id {}", categoryId);
        return service.getProductsCategoryId(categoryId);
    }

    @GetMapping(ApiPath.SEARCH)
    @ResponseStatus(HttpStatus.OK)
    public List<ProductDto> findProductsSearch(@RequestParam String query) {
        log.info("Запрос на получение списка продуктов по запросу {}", query);
        return service.getSearch(query);
    }
}
