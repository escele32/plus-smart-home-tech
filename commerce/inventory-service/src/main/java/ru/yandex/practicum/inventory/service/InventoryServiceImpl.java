package ru.yandex.practicum.inventory.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryServiceImpl implements InventoryService {
    InventoryRepository repository;

    private Inventory validateInventory(Long productId) {
        return repository.findByProductId(productId).orElseThrow(() -> {
            log.warn("Складской записи продукта с id {} не существует", productId);
            return new NotFoundException(format("Складской записи продукта с id %d не существует", productId));
        });
    }

    @Override
    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        log.debug("Добавление складской записи товара с id {}", request);
        Inventory inventory = repository.save(Inventory.builder()
                .productId(request.productId())
                .quantity(0)
                .reservedQuantity(0)
                .build()
        );
        log.info("Создана складская запись с id {}", inventory.getId());
        return InventoryMapper.toDto(inventory);
    }

    @Override
    @Transactional
    public InventoryDto update(UpdateInventoryRequest request) {
        log.debug("Обновление складской записи продукта с id {}", request.productId());
        Inventory inventory = validateInventory(request.productId());
        if (request.productId() != null) {
            inventory.setProductId(request.productId());
        }
        if (request.quantity() != null) {
            inventory.setQuantity(request.quantity());
        }
        log.info("Данные складской записи с id {} обновлены", inventory.getId());
        return InventoryMapper.toDto(repository.save(inventory));
    }

    @Override
    public List<InventoryDto> getAll() {
        List<Inventory> inventories = repository.findAll();
        log.info("Получение списка складских записей");
        return inventories.stream().map(InventoryMapper::toDto).toList();
    }

    @Override
    @Transactional
    public InventoryDto getByProductId(Long productId) {
        log.debug("Получение остатков по товару с id {}", productId);
        Inventory inventory = validateInventory(productId);
        log.info("Получены остатки по товару с id {}", productId);
        return InventoryMapper.toDto(inventory);
    }

    @Override
    @Transactional
    public ReserveResponse reserveProduct(ReserveRequest request) {
        log.debug("Резервируем товар с id {}", request.productId());
        Inventory inventory = validateInventory(request.productId());
        if (inventory.getAvailableQuantity() < request.quantity()) {
            log.warn("Товара с id {} нехватает на складе: остаток {}, запрошено {}",
                    request.productId(), inventory.getAvailableQuantity(), request.quantity());
            throw  new InsufficientStockException(format("Товара с id {} нехватает на складе: остаток {}, запрошено {}",
                    request.productId(), inventory.getAvailableQuantity(), request.quantity()));
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        InventoryDto dto = InventoryMapper.toDto(inventory);
        log.info("Товар с id {} успешно зарезервирован", dto.productId());
        return new ReserveResponse(true, dto.availableQuantity(), "Товар успешно зарезервирован");
    }
}
