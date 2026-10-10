package br.com.florir.controller;
import br.com.florir.dto.OrderDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.service.OrderService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;import java.util.List;
@RestController @RequestMapping("/api/v1") public class OrderController {
 private final OrderService service;public OrderController(OrderService s){service=s;}
 @GetMapping("/manager/orders") PageView<OrderView> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(page,size);}
 @GetMapping("/customer/orders") List<OrderView> mine(){return service.myOrders();}
 @GetMapping({"/manager/orders/{id}","/customer/orders/{id}"}) OrderView get(@PathVariable String id){return service.get(id);}
 @PostMapping({"/manager/orders","/customer/orders"}) @ResponseStatus(HttpStatus.CREATED) OrderView create(@Valid @RequestBody OrderInput data,@RequestHeader(value="Idempotency-Key",required=false) String key){return service.create(data,key);}
 @PostMapping("/manager/orders/{id}/confirm") OrderView confirm(@PathVariable String id){return service.confirm(id);}
 @PatchMapping("/manager/orders/{id}/status") OrderView status(@PathVariable String id,@Valid @RequestBody StatusInput data){return service.status(id,data.status());}
 @PostMapping("/manager/orders/{id}/receive") OrderView receive(@PathVariable String id){return service.receive(id);}
 @PostMapping("/customer/orders/{id}/simulate-payment") OrderView simulate(@PathVariable String id){return service.simulate(id);}
 @PostMapping({"/manager/orders/{id}/cancel","/customer/orders/{id}/cancel"}) OrderView cancel(@PathVariable String id){return service.cancel(id);}
 @PostMapping("/manager/installments/{id}/receive") OrderView pay(@PathVariable String id){return service.payInstallment(id);}
}
