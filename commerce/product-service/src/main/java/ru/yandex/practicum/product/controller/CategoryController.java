package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.service.CategoryService;
import ru.yandex.practicum.product.util.ApiPath;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API + ApiPath.CATEGORIES)
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {
    CategoryService service;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createDto(@Valid @RequestBody CreateCategoryRequest request) {
        log.info("Запрос на добавление категории {}", request);
        return service.create(request);
    }

    @GetMapping(ApiPath.ID)
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto findCategoryById(@PathVariable("id") Long categoryId) {
        log.info("Запрос на получение категории по id {}", categoryId);
        return service.getById(categoryId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryDto> findAll() {
        log.info("Запрос на получение списка категорий");
        return service.getAll();
    }
}
