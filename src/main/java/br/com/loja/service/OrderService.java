package br.com.loja.service;

import br.com.loja.dto.CatalogDtos.PageView;
import br.com.loja.dto.OrderDtos.*;
import br.com.loja.exception.ApiException;
import br.com.loja.model.*;
import br.com.loja.repository.*;
import br.com.loja.security.ActorService;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final CustomerRepository customerRepository;
  private final CouponRepository couponRepository;
  private final ProductPromotionRepository productPromotionRepository;
  private final InterestRateRepository interestRateRepository;
  private final InstallmentRepository installmentRepository;
  private final InventoryMovementRepository inventoryMovementRepository;
  private final FinancialMovementRepository financialMovementRepository;
  private final StatusHistoryRepository statusHistoryRepository;
  private final CustomerAddressRepository customerAddressRepository;
  private final ActorService actorService;
  private final AuditService auditService;
  private final Environment environment;

  public OrderView view(PurchaseOrder purchaseOrder) {
    return new OrderView(
        purchaseOrder.getId().toString(),
        purchaseOrder.getCustomer().getId().toString(),
        purchaseOrder.getCustomerName(),
        purchaseOrder.getCustomerPhone(),
        purchaseOrder.getStatus(),
        purchaseOrder.getPaymentStatus(),
        purchaseOrder.getPaymentMethod(),
        purchaseOrder.getStockEffect(),
        purchaseOrder.getSubtotal(),
        purchaseOrder.getDiscount(),
        purchaseOrder.getShipping(),
        purchaseOrder.getInterest(),
        purchaseOrder.getTotal(),
        purchaseOrder.getInstallmentCount(),
        purchaseOrder.getDelivery(),
        purchaseOrder.getAddressSnapshot(),
        purchaseOrder.getCreatedAt(),
        orderItemRepository.findByOrderIdOrderById(purchaseOrder.getId()).stream()
            .map(
                orderItem ->
                    new ItemView(
                        orderItem.getId().toString(),
                        orderItem.getProduct().getId().toString(),
                        orderItem.getProductName(),
                        orderItem.getBrandName(),
                        orderItem.getCategoryName(),
                        orderItem.getQuantity(),
                        orderItem.getUnitPrice()))
            .toList(),
        installmentRepository.findByOrderIdOrderByNumber(purchaseOrder.getId()).stream()
            .map(
                installment ->
                    new InstallmentView(
                        installment.getId().toString(),
                        installment.getNumber(),
                        installment.getAmount(),
                        installment.getDueDate(),
                        installment.getStatus(),
                        installment.getPaidAt()))
            .toList(),
        statusHistoryRepository.findByOrderIdOrderByCreatedAt(purchaseOrder.getId()).stream()
            .map(
                statusHistory ->
                    new HistoryView(
                        statusHistory.getPreviousStatus(),
                        statusHistory.getNextStatus(),
                        statusHistory.getCreatedAt()))
            .toList());
  }

  @Transactional(readOnly = true)
  public PageView<OrderView> list(int page, int size) {
    return PageView.from(
        purchaseOrderRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public List<OrderView> myOrders() {
    return purchaseOrderRepository
        .findByCustomerIdOrderByCreatedAtDesc(actorService.customer().getId())
        .stream()
        .map(this::view)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrderView get(String id) {
    var purchaseOrder =
        purchaseOrderRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actorService.owns(purchaseOrder.getCustomer());
    return view(purchaseOrder);
  }

  public OrderView create(OrderInput data, String rawKey) {
    Customer customer =
        "CUSTOMER".equals(actorService.role())
            ? actorService.customer()
            : customerRepository
                .findById(CatalogService.id(data.customerId()))
                .orElseThrow(ApiException::missing);
    String hash = AuthService.hash(data.toString() + "|" + customer.getId());
    String idempotencyKey = null;
    if (rawKey != null) {
      if (rawKey.isBlank() || rawKey.length() > 128)
        throw new ApiException(400, "Idempotency-Key inválida");
      idempotencyKey = actorService.role() + ":" + actorService.accountId() + ":" + rawKey;
      var prior = purchaseOrderRepository.findByIdempotencyKey(idempotencyKey);
      if (prior.isPresent()) {
        actorService.owns(prior.get().getCustomer());
        if (!hash.equals(prior.get().getRequestHash()))
          throw ApiException.conflict("Chave de idempotência já usada com outros dados");
        return view(prior.get());
      }
    }
    LocalDate today = LocalDate.now();
    Coupon coupon = null;
    BigDecimal couponDiscount = BigDecimal.ZERO;
    if (data.couponCode() != null && !data.couponCode().isBlank()) {
      var found =
          couponRepository
              .findByCode(data.couponCode().strip().toUpperCase(Locale.ROOT))
              .orElseThrow(() -> new ApiException(400, "Cupom inválido"));
      coupon = couponRepository.lockById(found.getId()).orElseThrow(ApiException::missing);
      if (today.isBefore(coupon.getValidFrom())
          || today.isAfter(coupon.getValidUntil())
          || (coupon.getUsageLimit() != null
              && purchaseOrderRepository.countByCouponIdAndStatusNot(coupon.getId(), "CANCELLED")
                  >= coupon.getUsageLimit())) throw new ApiException(400, "Cupom indisponível");
    }
    var quantities = new TreeMap<Long, Integer>();
    for (var orderItemInput : data.items())
      quantities.merge(
          CatalogService.id(orderItemInput.productId()), orderItemInput.quantity(), Math::addExact);
    var lines = new ArrayList<OrderItem>();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var entry : quantities.entrySet()) {
      var product = productRepository.lockById(entry.getKey()).orElseThrow(ApiException::missing);
      if (!product.getActive()) throw new ApiException(400, "Produto indisponível");
      if (entry.getValue() > product.getStock())
        throw ApiException.conflict("Estoque insuficiente");
      BigDecimal percent =
          productPromotionRepository.findByProductId(product.getId()).stream()
              .map(ProductPromotion::getPromotion)
              .filter(
                  promotion ->
                      !today.isBefore(promotion.getValidFrom())
                          && !today.isAfter(promotion.getValidUntil()))
              .map(Promotion::getPercent)
              .max(BigDecimal::compareTo)
              .orElse(BigDecimal.ZERO);
      BigDecimal price =
          PricingCalculator.money(
              product
                  .getPrice()
                  .multiply(BigDecimal.ONE.subtract(percent.divide(new BigDecimal("100")))));
      var orderItem = new OrderItem();
      orderItem.setProduct(product);
      orderItem.setQuantity(entry.getValue());
      orderItem.setUnitPrice(price);
      orderItem.setUnitCost(product.getCost());
      orderItem.setProductName(product.getName());
      orderItem.setBrandName(product.getBrand().getName());
      orderItem.setCategoryName(product.getCategory().getName());
      lines.add(orderItem);
      subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
    }
    if (coupon != null)
      couponDiscount =
          "PERCENT".equals(coupon.getType())
              ? PricingCalculator.money(
                  subtotal.multiply(coupon.getValue()).divide(new BigDecimal("100")))
              : coupon.getValue();
    int count = "INSTALLMENTS".equals(data.paymentMethod()) ? data.installmentCount() : 1;
    InterestRate rate =
        "INSTALLMENTS".equals(data.paymentMethod())
            ? interestRateRepository
                .findByInstallmentCount(count)
                .orElseThrow(() -> new ApiException(400, "Taxa de juros não configurada"))
            : null;
    var pricing =
        PricingCalculator.calculate(
            subtotal,
            couponDiscount,
            data.delivery() ? new BigDecimal("12.00") : BigDecimal.ZERO,
            rate == null ? BigDecimal.ZERO : rate.getPercent());
    if (pricing.total().signum() > 0 && "INSTALLMENTS".equals(data.paymentMethod()))
      PricingCalculator.split(pricing.total(), count);
    String address = null;
    if (data.delivery()) {
      if (data.addressId() == null) throw new ApiException(400, "Endereço obrigatório");
      var customerAddress =
          customerAddressRepository
              .findById(CatalogService.id(data.addressId()))
              .orElseThrow(ApiException::missing);
      if (!customerAddress.getCustomer().getId().equals(customer.getId()))
        throw ApiException.missing();
      address =
          customerAddress.getStreet()
              + ", "
              + customerAddress.getNumber()
              + ", "
              + Objects.toString(customerAddress.getComplement(), "")
              + " - "
              + customerAddress.getDistrict()
              + ", "
              + customerAddress.getCity()
              + "/"
              + customerAddress.getState()
              + " CEP "
              + customerAddress.getPostalCode();
    }
    var purchaseOrder = new PurchaseOrder();
    purchaseOrder.setCustomer(customer);
    purchaseOrder.setCustomerName(customer.getName());
    purchaseOrder.setCustomerPhone(customer.getPhone());
    purchaseOrder.setCoupon(coupon);
    purchaseOrder.setInterestRate(rate);
    purchaseOrder.setOrigin(actorService.role());
    purchaseOrder.setPaymentMethod(data.paymentMethod());
    purchaseOrder.setInstallmentCount(count);
    purchaseOrder.setDelivery(data.delivery());
    purchaseOrder.setAddressSnapshot(address);
    purchaseOrder.setSubtotal(pricing.subtotal());
    purchaseOrder.setDiscount(pricing.discount());
    purchaseOrder.setShipping(pricing.shipping());
    purchaseOrder.setInterest(pricing.interest());
    purchaseOrder.setTotal(pricing.total());
    purchaseOrder.setIdempotencyKey(idempotencyKey);
    purchaseOrder.setRequestHash(hash);
    purchaseOrderRepository.save(purchaseOrder);
    for (var orderItem : lines) {
      orderItem.setOrder(purchaseOrder);
      orderItemRepository.save(orderItem);
    }
    history(purchaseOrder, null, "NEW");
    auditService.record("CREATE", "PurchaseOrder", purchaseOrder.getId());
    return view(purchaseOrder);
  }

  private PurchaseOrder lock(String id) {
    var purchaseOrder =
        purchaseOrderRepository.lockById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actorService.owns(purchaseOrder.getCustomer());
    return purchaseOrder;
  }

  private void history(PurchaseOrder purchaseOrder, String previous, String next) {
    var statusHistory = new StatusHistory();
    statusHistory.setOrder(purchaseOrder);
    statusHistory.setPreviousStatus(previous);
    statusHistory.setNextStatus(next);
    if ("MANAGER".equals(actorService.role())) statusHistory.setManager(actorService.manager());
    statusHistoryRepository.save(statusHistory);
  }

  private void transition(PurchaseOrder purchaseOrder, String status) {
    String previous = purchaseOrder.getStatus();
    purchaseOrder.setStatus(status);
    history(purchaseOrder, previous, status);
    auditService.record("STATUS_" + status, "PurchaseOrder", purchaseOrder.getId());
  }

  private void confirm(PurchaseOrder purchaseOrder) {
    if (!"NEW".equals(purchaseOrder.getStatus())) {
      if (List.of("CONFIRMED", "PREPARING", "FINISHED").contains(purchaseOrder.getStatus())) return;
      throw ApiException.conflict("Pedido cancelado");
    }
    var lines =
        orderItemRepository.findByOrderIdOrderById(purchaseOrder.getId()).stream()
            .sorted(Comparator.comparing(orderItem -> orderItem.getProduct().getId()))
            .toList();
    for (var orderItem : lines) {
      var product =
          productRepository
              .lockById(orderItem.getProduct().getId())
              .orElseThrow(ApiException::missing);
      if (product.getStock() < orderItem.getQuantity())
        throw ApiException.conflict("Estoque insuficiente para confirmação");
      product.setStock(product.getStock() - orderItem.getQuantity());
      inventory(product, purchaseOrder, -orderItem.getQuantity(), "ORDER_OUT");
    }
    purchaseOrder.setStockEffect("APPLIED");
    if ("INSTALLMENTS".equals(purchaseOrder.getPaymentMethod())
        && purchaseOrder.getTotal().signum() > 0) {
      var amounts =
          PricingCalculator.split(purchaseOrder.getTotal(), purchaseOrder.getInstallmentCount());
      for (int installmentIndex = 0; installmentIndex < amounts.size(); installmentIndex++) {
        var installment = new Installment();
        installment.setOrder(purchaseOrder);
        installment.setNumber(installmentIndex + 1);
        installment.setAmount(amounts.get(installmentIndex));
        installment.setDueDate(LocalDate.now().plusMonths(installmentIndex + 1));
        installmentRepository.save(installment);
      }
    }
    if (purchaseOrder.getTotal().signum() == 0) purchaseOrder.setPaymentStatus("APPROVED");
    transition(purchaseOrder, "CONFIRMED");
  }

  private void inventory(Product product, PurchaseOrder purchaseOrder, int amount, String type) {
    var inventoryMovement = new InventoryMovement();
    inventoryMovement.setProduct(product);
    inventoryMovement.setOrder(purchaseOrder);
    inventoryMovement.setType(type);
    inventoryMovement.setQuantity(amount);
    inventoryMovement.setDescription(type);
    inventoryMovementRepository.save(inventoryMovement);
  }

  public OrderView confirm(String id) {
    var purchaseOrder = lock(id);
    confirm(purchaseOrder);
    return view(purchaseOrder);
  }

  public OrderView status(String id, String next) {
    var purchaseOrder = lock(id);
    if (purchaseOrder.getStatus().equals(next)) return view(purchaseOrder);
    boolean validTransition =
        "PREPARING".equals(next) && "CONFIRMED".equals(purchaseOrder.getStatus())
            || "FINISHED".equals(next)
                && List.of("CONFIRMED", "PREPARING").contains(purchaseOrder.getStatus());
    if (!validTransition) throw ApiException.conflict("Transição de status inválida");
    transition(purchaseOrder, next);
    return view(purchaseOrder);
  }

  private BigDecimal received(PurchaseOrder purchaseOrder) {
    return financialMovementRepository.findByOrderId(purchaseOrder.getId()).stream()
        .map(
            financialMovement ->
                "IN".equals(financialMovement.getType())
                    ? financialMovement.getAmount()
                    : financialMovement.getAmount().negate())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void movement(
      PurchaseOrder purchaseOrder,
      Installment installment,
      String type,
      BigDecimal amount,
      String description) {
    if (amount.signum() <= 0) return;
    var financialMovement = new FinancialMovement();
    financialMovement.setOrder(purchaseOrder);
    financialMovement.setInstallment(installment);
    financialMovement.setType(type);
    financialMovement.setAmount(amount);
    financialMovement.setDescription(description);
    financialMovementRepository.save(financialMovement);
  }

  public OrderView receive(String id) {
    var purchaseOrder = lock(id);
    if ("CANCELLED".equals(purchaseOrder.getStatus()))
      throw ApiException.conflict("Pedido cancelado");
    if ("INSTALLMENTS".equals(purchaseOrder.getPaymentMethod()))
      throw new ApiException(400, "Registre recebimento em cada parcela");
    confirm(purchaseOrder);
    if (!"APPROVED".equals(purchaseOrder.getPaymentStatus())) {
      movement(purchaseOrder, null, "IN", purchaseOrder.getTotal(), "Recebimento de pedido");
      purchaseOrder.setPaymentStatus("APPROVED");
      auditService.record("RECEIVE", "PurchaseOrder", purchaseOrder.getId());
    }
    return view(purchaseOrder);
  }

  public OrderView simulate(String id) {
    if (!environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test")))
      throw new ApiException(403, "Simulação indisponível");
    var purchaseOrder = lock(id);
    if ("INSTALLMENTS".equals(purchaseOrder.getPaymentMethod())) {
      confirm(purchaseOrder);
      if (purchaseOrder.getTotal().signum() > 0) purchaseOrder.setPaymentStatus("AUTHORIZED");
      return view(purchaseOrder);
    }
    return receive(id);
  }

  public OrderView cancel(String id) {
    var purchaseOrder = lock(id);
    if ("CANCELLED".equals(purchaseOrder.getStatus())) return view(purchaseOrder);
    if ("FINISHED".equals(purchaseOrder.getStatus()))
      throw ApiException.conflict("Pedido finalizado é terminal");
    if ("APPLIED".equals(purchaseOrder.getStockEffect())) {
      var lines =
          orderItemRepository.findByOrderIdOrderById(purchaseOrder.getId()).stream()
              .sorted(Comparator.comparing(orderItem -> orderItem.getProduct().getId()))
              .toList();
      for (var orderItem : lines) {
        var product =
            productRepository
                .lockById(orderItem.getProduct().getId())
                .orElseThrow(ApiException::missing);
        product.setStock(Math.addExact(product.getStock(), orderItem.getQuantity()));
        inventory(product, purchaseOrder, orderItem.getQuantity(), "CANCELLATION_IN");
      }
      purchaseOrder.setStockEffect("REVERSED");
    }
    var cash = received(purchaseOrder);
    if (cash.signum() > 0) {
      movement(purchaseOrder, null, "OUT", cash, "Estorno de cancelamento");
      purchaseOrder.setPaymentStatus("REFUNDED");
    } else {
      purchaseOrder.setPaymentStatus("CANCELLED");
    }
    installmentRepository.findByOrderIdOrderByNumber(purchaseOrder.getId()).stream()
        .filter(installment -> "PENDING".equals(installment.getStatus()))
        .forEach(installment -> installment.setStatus("CANCELLED"));
    transition(purchaseOrder, "CANCELLED");
    return view(purchaseOrder);
  }

  public OrderView payInstallment(String id) {
    var found =
        installmentRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    var purchaseOrder = lock(found.getOrder().getId().toString());
    var installment =
        installmentRepository.lockById(found.getId()).orElseThrow(ApiException::missing);
    if ("CANCELLED".equals(purchaseOrder.getStatus())
        || "CANCELLED".equals(installment.getStatus()))
      throw ApiException.conflict("Parcela cancelada");
    if ("PAID".equals(installment.getStatus())) return view(purchaseOrder);
    installment.setStatus("PAID");
    installment.setPaidAt(Instant.now());
    movement(
        purchaseOrder,
        installment,
        "IN",
        installment.getAmount(),
        "Recebimento de parcela " + installment.getNumber());
    if (installmentRepository.findByOrderIdOrderByNumber(purchaseOrder.getId()).stream()
        .allMatch(candidateInstallment -> "PAID".equals(candidateInstallment.getStatus())))
      purchaseOrder.setPaymentStatus("APPROVED");
    auditService.record("PAY_INSTALLMENT", "Installment", installment.getId());
    return view(purchaseOrder);
  }
}
