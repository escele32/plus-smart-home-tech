package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {

    CategoryDto create(CreateCategoryRequest request);

    CategoryDto getById(Long categoryId);

    List<CategoryDto> getAll();
}
