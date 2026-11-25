package ru.yandex.practicum.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.order.breaker.InventoryClientFallbackFactory;

@FeignClient(name = "inventory-service", fallbackFactory = InventoryClientFallbackFactory.class)
public interface InventoryClient {

    @PostMapping("/api/inventory/reserve")
    InventoryResponse reserveStock(@RequestBody InventoryRequest request);

    @PostMapping("/api/inventory/release")
    InventoryResponse releaseStock(@RequestBody InventoryRequest request);
}
