package ru.yandex.practicum.order.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {
    static String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    OrderRepository repository;

    @Override
    @Transactional
    public OrderDto create(CreateOrderRequest request) {
        log.debug("Добавление заказа {}", request);
        BigDecimal totalPrice = request.items().stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status(PENDING_CONFIRMATION)
                .statusDetails("Заказ требует проверки!")
                .totalPrice(totalPrice)
                .createdAt(LocalDateTime.now())
                .build();
        request.items().forEach(itemRequest -> {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(itemRequest.productId())
                    .productName(format("Товар %s ожидает проверки", itemRequest.productName()))
                    .quantity(itemRequest.quantity())
                    .price(itemRequest.price())
                    .build();
            order.getItems().add(item);
        });
        log.info("Заказ создан {}", order);
        return OrderMapper.toDto(repository.save(order));
    }

    @Override
    public OrderDto getById(Long orderId) {
        Order order = repository.findById(orderId).orElseThrow(() -> {
            log.warn("Заказ с id {} не найден", orderId);
            return new NotFoundException(format("Заказ с id %d не найден", orderId));
        });
        return OrderMapper.toDto(order);
    }

    @Override
    public List<OrderDto> getAll() {
        List<Order> orders = repository.findAll();
        log.info("Получение списка заказов");
        return orders.stream().map(OrderMapper::toDto).toList();
    }

    @Override
    public List<OrderDto> getOrdersByEmail(String email) {
        List<Order> orders = repository.findByCustomerEmail(email).stream().toList();
        log.info("Получение списка заказов по email {}", email);
        return orders.stream().map(OrderMapper::toDto).toList();
    }
}