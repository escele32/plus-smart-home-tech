package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {

    ProductDto create(CreateProductRequest request);

    ProductDto update(Long productId, UpdateProductRequest request);

    List<ProductDto> getAll();

    ProductDto getById(Long productId);

    List<ProductDto> getProductsCategoryId(Long categoryId);

    List<ProductDto> getSearch(String query);
}
