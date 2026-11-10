package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.breaker.ServiceCallResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderOrchestrationService {
    ProductClient productClient;
    InventoryClient inventoryClient;
    OrderService service;

    private ServiceCallResult<ProductDto> getProduct(Long productId) {
        try {
            ProductDto dto = productClient.getProductById(productId);
            if (!Boolean.TRUE.equals(dto.active())) {
                log.warn("Товар с id {} снят с продажи", productId);
                return new  ServiceCallResult.Failure<>(format("Товар с id %d снят с продажи", productId));
            }
            return new ServiceCallResult.Success<>(dto);
        } catch (FeignException.NotFound exception) {
            log.warn("Товар с id {} не найден", productId);
            return new ServiceCallResult.Failure<>(format("Товар с id %d не найден", productId));
        } catch (ProductServiceUnavailableException exception) {
            log.warn("Каталог не доступен");
            return new ServiceCallResult.Degraded<>("Каталог не доступен");
        } catch (FeignException exception) {
            log.warn("Не удалось получить данные товара c id {}", productId);
            return new ServiceCallResult.Degraded<>(format("Не удалось получить данные товара c id %d", productId));
        }
    }

    private ServiceCallResult<InventoryResponse> getReserve(InventoryRequest request) {
        try {
            return new ServiceCallResult.Success<>(inventoryClient.reserveStock(request));
        } catch (FeignException.NotFound exception) {
            log.warn("Складская запись для товара с id {} не найдена", request.productId());
            return new ServiceCallResult.Failure<>(format("Складская запись для товара с id %d не найдена",
                    request.productId()));
        } catch (FeignException.Conflict exception) {
            log.warn("Для заказа недостаточно товара с id {}", request.productId());
            return new ServiceCallResult.Failure<>(format("Для заказа недостаточно товара с id %d",
                    request.productId()));
        } catch (InventoryServiceUnavailableException exception) {
            log.warn("Склад временно недоступен");
            return new ServiceCallResult.Degraded<>("Склад временно недоступен");
        } catch (FeignException exception) {
            log.warn("Товар с id {} не удалось зарезервировать", request.productId());
            return new ServiceCallResult.Degraded<>(format("Товар с id %d не удалось зарезервировать",
                    request.productId()));
        }
    }

    private ProductDto requireProduct(Long productId) {
        ServiceCallResult<ProductDto> result = getProduct(productId);
        return switch (result) {
            case ServiceCallResult.Success<ProductDto> success -> success.value();
            case ServiceCallResult.Failure<ProductDto> failure -> throw new OrderProcessingException(failure.message());
            case ServiceCallResult.Degraded<ProductDto> degraded ->
                    throw new ProductServiceUnavailableException(degraded.reason());
            default -> throw new OrderProcessingException("Неизвестный результат проверки товара");
        };
    }

    private void reserve(InventoryRequest request, List<InventoryRequest> reserved) {
        ServiceCallResult<InventoryResponse> result = getReserve(request);
        switch (result) {
            case ServiceCallResult.Success<InventoryResponse> success -> {
                // Добавляем только после подтверждённого резервирования.
                reserved.add(request);
                return;
            }
            case ServiceCallResult.Failure<InventoryResponse> failure ->
                    throw new OrderProcessingException(failure.message());
            case ServiceCallResult.Degraded<InventoryResponse> degraded ->
                    throw new InventoryServiceUnavailableException(degraded.reason());
            default -> {
            }
        }
        throw new OrderProcessingException("Неизвестный результат резервирования товара");
    }

    private void getRelease(List<InventoryRequest> requestReserve) {
        for (int i = requestReserve.size() - 1; i >= 0; i--) {
            InventoryRequest request = requestReserve.get(i);
            try {
                inventoryClient.releaseStock(request);
            } catch (FeignException exception) {
                log.warn("Не удалось отменить резерв {} товара с id {}", request.quantity(), request.productId());
                throw new OrderProcessingException(format("Не удалось отменить резерв %d товара с id %d",
                        request.quantity(), request.productId()));
            }
        }
    }

    private OrderItemRequest getSnapshot(OrderItemRequest itemDto, ProductDto productDto) {
        return new OrderItemRequest(itemDto.productId(), productDto.name(), itemDto.quantity(), productDto.price());
    }

    private Map<Long, Integer> getAggregate(List<OrderItemRequest> items) {
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        items.forEach(itemRequest -> quantityMap.merge(itemRequest.productId(), itemRequest.quantity(),
                Integer::sum));
        return quantityMap;
    }

    public OrderDto createOrder(CreateOrderRequest request) {
        Map<Long, Integer> quantityMap = getAggregate(request.items());
        Map<Long, ProductDto> productMap = new LinkedHashMap<>();
        List<InventoryRequest> reserveList = new ArrayList<>();
        try {
            quantityMap.keySet().forEach(productId -> productMap.put(productId, requireProduct(productId)));
            quantityMap.forEach((key, value) -> {
                InventoryRequest inventoryRequest = new InventoryRequest(key, value);
                reserve(inventoryRequest, reserveList);
            });
            List<OrderItemRequest> itemsSnapshotList = request.items()
                    .stream()
                    .map(itemRequest -> getSnapshot(itemRequest,
                            productMap.get(itemRequest.productId())))
                    .toList();
            CreateOrderRequest orderRequest = new CreateOrderRequest(
                    request.customerName(),
                    request.customerEmail(),
                    itemsSnapshotList
            );
            return service.create(orderRequest);
        } catch (RuntimeException exception) {
            log.warn("Не удалось завершить создание заказа, отмена резервирования");
            if (exception instanceof OrderProcessingException
                    || exception instanceof ProductServiceUnavailableException
                    || exception instanceof InventoryServiceUnavailableException) {
                throw exception;
            }
            getRelease(reserveList);
            throw new OrderProcessingException("Не удалось завершить создание заказа, попробуйте зарезервировать позже");
        }
    }
}
