package ru.yandex.practicum.inventory.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.service.InventoryService;
import ru.yandex.practicum.inventory.util.ApiPath;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API + ApiPath.INVENTORY)
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {
    InventoryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDto createInventory(@Valid @RequestBody UpdateInventoryRequest request) {
        log.info("Запрос на добавление скаладской записи {}", request);
        return service.create(request);
    }

    @PutMapping
    public InventoryDto updateInventory(@Valid @RequestBody UpdateInventoryRequest request) {
        log.info("Запрос на обновление скаладской записи {}", request);
        return service.update(request);
    }

    @GetMapping
    public List<InventoryDto> findAll() {
        log.info("Запрос на получение списка складских записей");
        return service.getAll();
    }

    @GetMapping(ApiPath.PRODUCT_ID)
    public Integer findProductBalances(@PathVariable Long productId) {
        log.info("Запрос на получение остатков по товару с id {}", productId);
        return service.getProductBalances(productId);
    }

    @PostMapping(ApiPath.RESERVE)
    public ReserveResponse findReserveProduct(@Valid @RequestBody ReserveRequest request) {
        log.info("Запрос на резервирование товара с id {}", request.productId());
        return service.reserveProduct(request);
    }
}
