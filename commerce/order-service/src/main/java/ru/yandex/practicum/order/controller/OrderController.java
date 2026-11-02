package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.service.OrderService;
import ru.yandex.practicum.order.util.ApiPath;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API + ApiPath.ORDERS)
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {
    OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Запрос на создание заказа {}", request);
        return service.create(request);
    }

    @GetMapping(ApiPath.ID)
    public OrderDto findById(@PathVariable("id") Long orderId) {
        log.info("Запрос на получение заказа по id {}", orderId);
        return service.getById(orderId);
    }

    @GetMapping
    public List<OrderDto> findAll() {
        log.info("Запрос на получение списка заказов");
        return service.getAll();
    }

    @GetMapping(ApiPath.BY_EMAIL)
    public List<OrderDto> findOrdersByEmail(@RequestParam String email) {
        log.info("Запрос на получение списка заказов по email {}", email);
        return service.getOrdersByEmail(email);
    }
}
