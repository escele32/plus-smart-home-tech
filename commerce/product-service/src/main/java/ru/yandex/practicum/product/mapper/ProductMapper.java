package ru.yandex.practicum.product.mapper;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.entity.Product;

@UtilityClass
public class ProductMapper {

    public ProductDto toDto(Product product) {
        CategoryDto categoryDto = new CategoryDto(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription()
        );
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                categoryDto,
                product.getImageUrl(),
                product.getActive()
        );
    }
}
