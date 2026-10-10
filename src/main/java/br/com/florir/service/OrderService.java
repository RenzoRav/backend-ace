package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.OrderDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import br.com.florir.security.ActorService;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {
  private final PurchaseOrderRepository orders;
  private final OrderItemRepository items;
  private final ProductRepository products;
  private final CustomerRepository customers;
  private final CouponRepository coupons;
  private final ProductPromotionRepository promotions;
  private final InterestRateRepository rates;
  private final InstallmentRepository installments;
  private final InventoryMovementRepository stock;
  private final FinancialMovementRepository finance;
  private final StatusHistoryRepository histories;
  private final CustomerAddressRepository addresses;
  private final ActorService actor;
  private final AuditService audit;
  private final Environment environment;

  public OrderService(
      PurchaseOrderRepository o,
      OrderItemRepository i,
      ProductRepository p,
      CustomerRepository c,
      CouponRepository co,
      ProductPromotionRepository pr,
      InterestRateRepository r,
      InstallmentRepository in,
      InventoryMovementRepository st,
      FinancialMovementRepository f,
      StatusHistoryRepository h,
      CustomerAddressRepository a,
      ActorService ac,
      AuditService au,
      Environment env) {
    orders = o;
    items = i;
    products = p;
    customers = c;
    coupons = co;
    promotions = pr;
    rates = r;
    installments = in;
    stock = st;
    finance = f;
    histories = h;
    addresses = a;
    actor = ac;
    audit = au;
    environment = env;
  }

  public OrderView view(PurchaseOrder o) {
    return new OrderView(
        o.getId().toString(),
        o.getCustomer().getId().toString(),
        o.getCustomerName(),
        o.getCustomerPhone(),
        o.getStatus(),
        o.getPaymentStatus(),
        o.getPaymentMethod(),
        o.getStockEffect(),
        o.getSubtotal(),
        o.getDiscount(),
        o.getShipping(),
        o.getInterest(),
        o.getTotal(),
        o.getInstallmentCount(),
        o.getDelivery(),
        o.getAddressSnapshot(),
        o.getCreatedAt(),
        items.findByOrderIdOrderById(o.getId()).stream()
            .map(
                i ->
                    new ItemView(
                        i.getId().toString(),
                        i.getProduct().getId().toString(),
                        i.getProductName(),
                        i.getBrandName(),
                        i.getCategoryName(),
                        i.getQuantity(),
                        i.getUnitPrice()))
            .toList(),
        installments.findByOrderIdOrderByNumber(o.getId()).stream()
            .map(
                i ->
                    new InstallmentView(
                        i.getId().toString(),
                        i.getNumber(),
                        i.getAmount(),
                        i.getDueDate(),
                        i.getStatus(),
                        i.getPaidAt()))
            .toList(),
        histories.findByOrderIdOrderByCreatedAt(o.getId()).stream()
            .map(h -> new HistoryView(h.getPreviousStatus(), h.getNextStatus(), h.getCreatedAt()))
            .toList());
  }

  @Transactional(readOnly = true)
  public PageView<OrderView> list(int page, int size) {
    return PageView.from(orders.findAll(CatalogService.page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public List<OrderView> myOrders() {
    return orders.findByCustomerIdOrderByCreatedAtDesc(actor.customer().getId()).stream()
        .map(this::view)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrderView get(String id) {
    var o = orders.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actor.owns(o.getCustomer());
    return view(o);
  }

  public OrderView create(OrderInput data, String rawKey) {
    Customer c =
        "CUSTOMER".equals(actor.role())
            ? actor.customer()
            : customers
                .findById(CatalogService.id(data.customerId()))
                .orElseThrow(ApiException::missing);
    String hash = AuthService.hash(data.toString() + "|" + c.getId());
    String key = null;
    if (rawKey != null) {
      if (rawKey.isBlank() || rawKey.length() > 128)
        throw new ApiException(400, "Idempotency-Key inválida");
      key = actor.role() + ":" + actor.accountId() + ":" + rawKey;
      var prior = orders.findByIdempotencyKey(key);
      if (prior.isPresent()) {
        actor.owns(prior.get().getCustomer());
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
          coupons
              .findByCode(data.couponCode().strip().toUpperCase(Locale.ROOT))
              .orElseThrow(() -> new ApiException(400, "Cupom inválido"));
      coupon = coupons.lockById(found.getId()).orElseThrow(ApiException::missing);
      if (today.isBefore(coupon.getValidFrom())
          || today.isAfter(coupon.getValidUntil())
          || (coupon.getUsageLimit() != null
              && orders.countByCouponIdAndStatusNot(coupon.getId(), "CANCELLED")
                  >= coupon.getUsageLimit())) throw new ApiException(400, "Cupom indisponível");
    }
    var quantities = new TreeMap<Long, Integer>();
    for (var i : data.items())
      quantities.merge(CatalogService.id(i.productId()), i.quantity(), Math::addExact);
    var lines = new ArrayList<OrderItem>();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var entry : quantities.entrySet()) {
      var p = products.lockById(entry.getKey()).orElseThrow(ApiException::missing);
      if (!p.getActive()) throw new ApiException(400, "Produto indisponível");
      if (entry.getValue() > p.getStock()) throw ApiException.conflict("Estoque insuficiente");
      BigDecimal percent =
          promotions.findByProductId(p.getId()).stream()
              .map(ProductPromotion::getPromotion)
              .filter(
                  pr -> !today.isBefore(pr.getValidFrom()) && !today.isAfter(pr.getValidUntil()))
              .map(Promotion::getPercent)
              .max(BigDecimal::compareTo)
              .orElse(BigDecimal.ZERO);
      BigDecimal price =
          PricingCalculator.money(
              p.getPrice()
                  .multiply(BigDecimal.ONE.subtract(percent.divide(new BigDecimal("100")))));
      var i = new OrderItem();
      i.setProduct(p);
      i.setQuantity(entry.getValue());
      i.setUnitPrice(price);
      i.setUnitCost(p.getCost());
      i.setProductName(p.getName());
      i.setBrandName(p.getBrand().getName());
      i.setCategoryName(p.getCategory().getName());
      lines.add(i);
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
            ? rates
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
      var a =
          addresses
              .findById(CatalogService.id(data.addressId()))
              .orElseThrow(ApiException::missing);
      if (!a.getCustomer().getId().equals(c.getId())) throw ApiException.missing();
      address =
          a.getStreet()
              + ", "
              + a.getNumber()
              + ", "
              + Objects.toString(a.getComplement(), "")
              + " - "
              + a.getDistrict()
              + ", "
              + a.getCity()
              + "/"
              + a.getState()
              + " CEP "
              + a.getPostalCode();
    }
    var o = new PurchaseOrder();
    o.setCustomer(c);
    o.setCustomerName(c.getName());
    o.setCustomerPhone(c.getPhone());
    o.setCoupon(coupon);
    o.setInterestRate(rate);
    o.setOrigin(actor.role());
    o.setPaymentMethod(data.paymentMethod());
    o.setInstallmentCount(count);
    o.setDelivery(data.delivery());
    o.setAddressSnapshot(address);
    o.setSubtotal(pricing.subtotal());
    o.setDiscount(pricing.discount());
    o.setShipping(pricing.shipping());
    o.setInterest(pricing.interest());
    o.setTotal(pricing.total());
    o.setIdempotencyKey(key);
    o.setRequestHash(hash);
    orders.save(o);
    for (var i : lines) {
      i.setOrder(o);
      items.save(i);
    }
    history(o, null, "NEW");
    audit.record("CREATE", "PurchaseOrder", o.getId());
    return view(o);
  }

  private PurchaseOrder lock(String id) {
    var o = orders.lockById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actor.owns(o.getCustomer());
    return o;
  }

  private void history(PurchaseOrder o, String previous, String next) {
    var h = new StatusHistory();
    h.setOrder(o);
    h.setPreviousStatus(previous);
    h.setNextStatus(next);
    if ("MANAGER".equals(actor.role())) h.setManager(actor.manager());
    histories.save(h);
  }

  private void transition(PurchaseOrder o, String status) {
    String previous = o.getStatus();
    o.setStatus(status);
    history(o, previous, status);
    audit.record("STATUS_" + status, "PurchaseOrder", o.getId());
  }

  private void confirm(PurchaseOrder o) {
    if (!"NEW".equals(o.getStatus())) {
      if (List.of("CONFIRMED", "PREPARING", "FINISHED").contains(o.getStatus())) return;
      throw ApiException.conflict("Pedido cancelado");
    }
    var lines =
        items.findByOrderIdOrderById(o.getId()).stream()
            .sorted(Comparator.comparing(i -> i.getProduct().getId()))
            .toList();
    for (var i : lines) {
      var p = products.lockById(i.getProduct().getId()).orElseThrow(ApiException::missing);
      if (p.getStock() < i.getQuantity())
        throw ApiException.conflict("Estoque insuficiente para confirmação");
      p.setStock(p.getStock() - i.getQuantity());
      inventory(p, o, -i.getQuantity(), "ORDER_OUT");
    }
    o.setStockEffect("APPLIED");
    if ("INSTALLMENTS".equals(o.getPaymentMethod()) && o.getTotal().signum() > 0) {
      var amounts = PricingCalculator.split(o.getTotal(), o.getInstallmentCount());
      for (int n = 0; n < amounts.size(); n++) {
        var i = new Installment();
        i.setOrder(o);
        i.setNumber(n + 1);
        i.setAmount(amounts.get(n));
        i.setDueDate(LocalDate.now().plusMonths(n + 1));
        installments.save(i);
      }
    }
    if (o.getTotal().signum() == 0) o.setPaymentStatus("APPROVED");
    transition(o, "CONFIRMED");
  }

  private void inventory(Product p, PurchaseOrder o, int amount, String type) {
    var m = new InventoryMovement();
    m.setProduct(p);
    m.setOrder(o);
    m.setType(type);
    m.setQuantity(amount);
    m.setDescription(type);
    stock.save(m);
  }

  public OrderView confirm(String id) {
    var o = lock(id);
    confirm(o);
    return view(o);
  }

  public OrderView status(String id, String next) {
    var o = lock(id);
    if (o.getStatus().equals(next)) return view(o);
    boolean ok =
        "PREPARING".equals(next) && "CONFIRMED".equals(o.getStatus())
            || "FINISHED".equals(next) && List.of("CONFIRMED", "PREPARING").contains(o.getStatus());
    if (!ok) throw ApiException.conflict("Transição de status inválida");
    transition(o, next);
    return view(o);
  }

  private BigDecimal received(PurchaseOrder o) {
    return finance.findByOrderId(o.getId()).stream()
        .map(m -> "IN".equals(m.getType()) ? m.getAmount() : m.getAmount().negate())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void movement(
      PurchaseOrder o, Installment i, String type, BigDecimal amount, String description) {
    if (amount.signum() <= 0) return;
    var m = new FinancialMovement();
    m.setOrder(o);
    m.setInstallment(i);
    m.setType(type);
    m.setAmount(amount);
    m.setDescription(description);
    finance.save(m);
  }

  public OrderView receive(String id) {
    var o = lock(id);
    if ("CANCELLED".equals(o.getStatus())) throw ApiException.conflict("Pedido cancelado");
    if ("INSTALLMENTS".equals(o.getPaymentMethod()))
      throw new ApiException(400, "Registre recebimento em cada parcela");
    confirm(o);
    if (!"APPROVED".equals(o.getPaymentStatus())) {
      movement(o, null, "IN", o.getTotal(), "Recebimento de pedido");
      o.setPaymentStatus("APPROVED");
      audit.record("RECEIVE", "PurchaseOrder", o.getId());
    }
    return view(o);
  }

  public OrderView simulate(String id) {
    if (!environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test")))
      throw new ApiException(403, "Simulação indisponível");
    var o = lock(id);
    if ("INSTALLMENTS".equals(o.getPaymentMethod())) {
      confirm(o);
      if (o.getTotal().signum() > 0) o.setPaymentStatus("AUTHORIZED");
      return view(o);
    }
    return receive(id);
  }

  public OrderView cancel(String id) {
    var o = lock(id);
    if ("CANCELLED".equals(o.getStatus())) return view(o);
    if ("FINISHED".equals(o.getStatus()))
      throw ApiException.conflict("Pedido finalizado é terminal");
    if ("APPLIED".equals(o.getStockEffect())) {
      var lines =
          items.findByOrderIdOrderById(o.getId()).stream()
              .sorted(Comparator.comparing(i -> i.getProduct().getId()))
              .toList();
      for (var i : lines) {
        var p = products.lockById(i.getProduct().getId()).orElseThrow(ApiException::missing);
        p.setStock(Math.addExact(p.getStock(), i.getQuantity()));
        inventory(p, o, i.getQuantity(), "CANCELLATION_IN");
      }
      o.setStockEffect("REVERSED");
    }
    var cash = received(o);
    if (cash.signum() > 0) {
      movement(o, null, "OUT", cash, "Estorno de cancelamento");
      o.setPaymentStatus("REFUNDED");
    } else {
      o.setPaymentStatus("CANCELLED");
    }
    installments.findByOrderIdOrderByNumber(o.getId()).stream()
        .filter(i -> "PENDING".equals(i.getStatus()))
        .forEach(i -> i.setStatus("CANCELLED"));
    transition(o, "CANCELLED");
    return view(o);
  }

  public OrderView payInstallment(String id) {
    var found = installments.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    var o = lock(found.getOrder().getId().toString());
    var i = installments.lockById(found.getId()).orElseThrow(ApiException::missing);
    if ("CANCELLED".equals(o.getStatus()) || "CANCELLED".equals(i.getStatus()))
      throw ApiException.conflict("Parcela cancelada");
    if ("PAID".equals(i.getStatus())) return view(o);
    i.setStatus("PAID");
    i.setPaidAt(Instant.now());
    movement(o, i, "IN", i.getAmount(), "Recebimento de parcela " + i.getNumber());
    if (installments.findByOrderIdOrderByNumber(o.getId()).stream()
        .allMatch(x -> "PAID".equals(x.getStatus()))) o.setPaymentStatus("APPROVED");
    audit.record("PAY_INSTALLMENT", "Installment", i.getId());
    return view(o);
  }
}
