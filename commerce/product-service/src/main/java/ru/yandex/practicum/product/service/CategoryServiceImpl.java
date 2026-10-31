package ru.yandex.practicum.product.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.CategoryMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {
    CategoryRepository repository;

    @Override
    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        log.debug("Добавление категории {}", request);
        Category category = repository.save(Category.builder()
                .name(request.name())
                .description(request.description())
                .build()
        );
        log.info("Создание категории {}", category);
        return CategoryMapper.toDto(category);
    }

    @Override
    public CategoryDto getById(Long categoryId) {
        Category category = repository.findById(categoryId).orElseThrow(() -> {
            log.warn("Категории с id {} не существует", categoryId);
            return new NotFoundException(format("Категории с id %d не существует", categoryId));
        });
        log.info("Получение категории по id {}", category.getId());
        return CategoryMapper.toDto(category);
    }

    @Override
    public List<CategoryDto> getAll() {
        List<Category> categories = repository.findAll();
        log.info("Получен список категорий");
        return categories.stream().map(CategoryMapper::toDto).toList();
    }
}
