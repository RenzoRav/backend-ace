import fs from 'node:fs';import path from 'node:path';const put=(p,s)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};const java=(p,s)=>put('src/main/java/br/com/florir/'+p+'.java',s);
java('dto/CampaignDtos',`package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.*;import java.util.List;
public final class CampaignDtos {
 public record CouponInput(@NotBlank @Size(max=255) String code,@NotBlank @Pattern(regexp="FIXED|PERCENT") String type,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal value,@NotNull LocalDate validFrom,@NotNull LocalDate validUntil,@Min(1) Integer usageLimit){}
 public record CouponView(String id,String code,String type,BigDecimal value,LocalDate validFrom,LocalDate validUntil,Integer usageLimit,long usageCount){}
 public record PromotionInput(@NotBlank @Size(max=255) String name,@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal percent,@NotNull LocalDate validFrom,@NotNull LocalDate validUntil,@NotEmpty @Size(max=100) List<@NotBlank String> productIds){}
 public record PromotionView(String id,String name,BigDecimal percent,LocalDate validFrom,LocalDate validUntil,List<String> productIds){}
 public record RateInput(@Min(1) @Max(12) int installmentCount,@NotNull @DecimalMin("0") @Digits(integer=5,fraction=2) BigDecimal percent){}
 public record RateView(String id,int installmentCount,BigDecimal percent){}
}
`);
java('service/CampaignService',`package br.com.florir.service;
import br.com.florir.dto.CampaignDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.model.*;import br.com.florir.repository.*;import br.com.florir.exception.ApiException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.data.domain.Sort;import java.util.*;
@Service @Transactional public class CampaignService {
 private final CouponRepository coupons;private final PromotionRepository promotions;private final ProductPromotionRepository links;private final ProductRepository products;private final InterestRateRepository rates;private final PurchaseOrderRepository orders;private final AuditService audit;
 public CampaignService(CouponRepository c,PromotionRepository p,ProductPromotionRepository l,ProductRepository pr,InterestRateRepository r,PurchaseOrderRepository o,AuditService a){coupons=c;promotions=p;links=l;products=pr;rates=r;orders=o;audit=a;}
 public CouponView view(Coupon c){return new CouponView(c.getId().toString(),c.getCode(),c.getType(),c.getValue(),c.getValidFrom(),c.getValidUntil(),c.getUsageLimit(),orders.countByCouponIdAndStatusNot(c.getId(),"CANCELLED"));}
 public PromotionView view(Promotion p){return new PromotionView(p.getId().toString(),p.getName(),p.getPercent(),p.getValidFrom(),p.getValidUntil(),links.findByPromotionId(p.getId()).stream().map(l->l.getProduct().getId().toString()).toList());}
 @Transactional(readOnly=true) public PageView<CouponView> coupons(int page,int size){return PageView.from(coupons.findAll(CatalogService.page(page,size)).map(this::view));}
 @Transactional(readOnly=true) public PageView<PromotionView> promotions(int page,int size){return PageView.from(promotions.findAll(CatalogService.page(page,size)).map(this::view));}
 public CouponView coupon(String id,CouponInput d){if(d.validUntil().isBefore(d.validFrom())||("PERCENT".equals(d.type())&&d.value().compareTo(new java.math.BigDecimal("100"))>0))throw new ApiException(400,"Período ou percentual inválido");var c=id==null?new Coupon():coupons.lockById(CatalogService.id(id)).orElseThrow(ApiException::missing);c.setCode(d.code().strip().toUpperCase(Locale.ROOT));c.setType(d.type());c.setValue(d.value());c.setValidFrom(d.validFrom());c.setValidUntil(d.validUntil());c.setUsageLimit(d.usageLimit());coupons.save(c);audit.record(id==null?"CREATE":"UPDATE","Coupon",c.getId());return view(c);}
 public void deleteCoupon(String id){var c=coupons.lockById(CatalogService.id(id)).orElseThrow(ApiException::missing);audit.record("DELETE","Coupon",c.getId());coupons.delete(c);}
 public PromotionView promotion(String id,PromotionInput d){if(d.validUntil().isBefore(d.validFrom()))throw new ApiException(400,"Período inválido");var p=id==null?new Promotion():promotions.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);p.setName(d.name().strip());p.setPercent(d.percent());p.setValidFrom(d.validFrom());p.setValidUntil(d.validUntil());promotions.save(p);if(id!=null){links.deleteAll(links.findByPromotionId(p.getId()));links.flush();}for(String productId:new LinkedHashSet<>(d.productIds())){var l=new ProductPromotion();l.setProduct(products.findById(CatalogService.id(productId)).orElseThrow(ApiException::missing));l.setPromotion(p);links.save(l);}audit.record(id==null?"CREATE":"UPDATE","Promotion",p.getId());return view(p);}
 public void deletePromotion(String id){var p=promotions.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);links.deleteAll(links.findByPromotionId(p.getId()));audit.record("DELETE","Promotion",p.getId());promotions.delete(p);}
 @Transactional(readOnly=true) public List<RateView> rates(){return rates.findAll(Sort.by("installmentCount")).stream().map(r->new RateView(r.getId().toString(),r.getInstallmentCount(),r.getPercent())).toList();}
 public RateView rate(RateInput d){var r=rates.findByInstallmentCount(d.installmentCount()).orElseGet(InterestRate::new);r.setInstallmentCount(d.installmentCount());r.setPercent(d.percent());rates.save(r);audit.record("UPDATE","InterestRate",r.getId());return new RateView(r.getId().toString(),r.getInstallmentCount(),r.getPercent());}
}
`);
let p='src/main/java/br/com/florir/repository/ProductPromotionRepository.java';let s=fs.readFileSync(p,'utf8');fs.writeFileSync(p,s.replace('extends JpaRepository<ProductPromotion,Long> {','extends JpaRepository<ProductPromotion,Long> { java.util.List<ProductPromotion> findByPromotionId(Long id);'));
java('controller/CampaignController',`package br.com.florir.controller;
import br.com.florir.dto.CampaignDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.service.CampaignService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;import java.util.List;
@RestController @RequestMapping("/api/v1/manager") public class CampaignController {
 private final CampaignService service;public CampaignController(CampaignService s){service=s;}
 @GetMapping("/coupons") PageView<CouponView> coupons(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.coupons(page,size);}
 @PostMapping("/coupons") @ResponseStatus(HttpStatus.CREATED) CouponView createCoupon(@Valid @RequestBody CouponInput d){return service.coupon(null,d);}
 @PutMapping("/coupons/{id}") CouponView updateCoupon(@PathVariable String id,@Valid @RequestBody CouponInput d){return service.coupon(id,d);}
 @DeleteMapping("/coupons/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCoupon(@PathVariable String id){service.deleteCoupon(id);}
 @GetMapping("/promotions") PageView<PromotionView> promotions(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.promotions(page,size);}
 @PostMapping("/promotions") @ResponseStatus(HttpStatus.CREATED) PromotionView createPromotion(@Valid @RequestBody PromotionInput d){return service.promotion(null,d);}
 @PutMapping("/promotions/{id}") PromotionView updatePromotion(@PathVariable String id,@Valid @RequestBody PromotionInput d){return service.promotion(id,d);}
 @DeleteMapping("/promotions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deletePromotion(@PathVariable String id){service.deletePromotion(id);}
 @GetMapping("/interest-rates") List<RateView> rates(){return service.rates();}
 @PutMapping("/interest-rates") RateView rate(@Valid @RequestBody RateInput d){return service.rate(d);}
}
`);
java('dto/FinanceDtos',`package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;
public final class FinanceDtos {
 public record EntryInput(@NotBlank @Pattern(regexp="IN|OUT") String type,@NotBlank @Size(max=255) String description,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount){}
 public record EntryView(String id,String orderId,String installmentId,String type,String description,BigDecimal amount,Instant date){}
 public record StockView(String id,String productId,String orderId,String type,int quantity,String description,Instant date){}
 public record AuditView(String id,String managerId,String managerName,String action,String entity,String entityId,Instant date){}
 public record Summary(long customers,long products,long orders,BigDecimal received,BigDecimal outgoing,BigDecimal balance,BigDecimal stockCost){}
}
`);
p='src/main/java/br/com/florir/repository/FinancialMovementRepository.java';s=fs.readFileSync(p,'utf8');fs.writeFileSync(p,s.replace('extends JpaRepository<FinancialMovement,Long> {','extends JpaRepository<FinancialMovement,Long> { @Query("select coalesce(sum(m.amount),0) from FinancialMovement m where m.type = :type") java.math.BigDecimal sumAmount(@org.springframework.data.repository.query.Param("type") String type);'));
p='src/main/java/br/com/florir/repository/ProductRepository.java';s=fs.readFileSync(p,'utf8');fs.writeFileSync(p,s.replace('extends JpaRepository<Product,Long> {','extends JpaRepository<Product,Long> { @Query("select coalesce(sum(p.cost * p.stock),0) from Product p") java.math.BigDecimal stockCost();'));
java('service/FinanceService',`package br.com.florir.service;
import br.com.florir.dto.FinanceDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.model.*;import br.com.florir.repository.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service @Transactional public class FinanceService {
 private final FinancialMovementRepository entries;private final InventoryMovementRepository stock;private final AuditLogRepository logs;private final CustomerRepository customers;private final ProductRepository products;private final PurchaseOrderRepository orders;private final AuditService audit;
 public FinanceService(FinancialMovementRepository e,InventoryMovementRepository st,AuditLogRepository l,CustomerRepository c,ProductRepository p,PurchaseOrderRepository o,AuditService a){entries=e;stock=st;logs=l;customers=c;products=p;orders=o;audit=a;}
 private EntryView view(FinancialMovement m){return new EntryView(m.getId().toString(),m.getOrder()==null?null:m.getOrder().getId().toString(),m.getInstallment()==null?null:m.getInstallment().getId().toString(),m.getType(),m.getDescription(),m.getAmount(),m.getCreatedAt());}
 @Transactional(readOnly=true) public PageView<EntryView> entries(int page,int size){return PageView.from(entries.findAll(CatalogService.page(page,size)).map(this::view));}
 public EntryView entry(EntryInput d){var m=new FinancialMovement();m.setType(d.type());m.setDescription(d.description());m.setAmount(PricingCalculator.money(d.amount()));entries.save(m);audit.record("CREATE_MANUAL","FinancialMovement",m.getId());return view(m);}
 @Transactional(readOnly=true) public PageView<StockView> stock(int page,int size){return PageView.from(stock.findAll(CatalogService.page(page,size)).map(m->new StockView(m.getId().toString(),m.getProduct().getId().toString(),m.getOrder()==null?null:m.getOrder().getId().toString(),m.getType(),m.getQuantity(),m.getDescription(),m.getCreatedAt())));}
 @Transactional(readOnly=true) public PageView<AuditView> audit(int page,int size){return PageView.from(logs.findAll(CatalogService.page(page,size)).map(l->new AuditView(l.getId().toString(),l.getManager().getId().toString(),l.getManager().getName(),l.getAction(),l.getEntity(),l.getEntityId(),l.getCreatedAt())));}
 @Transactional(readOnly=true) public Summary summary(){var in=entries.sumAmount("IN");var out=entries.sumAmount("OUT");return new Summary(customers.count(),products.count(),orders.count(),PricingCalculator.money(in),PricingCalculator.money(out),PricingCalculator.money(in.subtract(out)),PricingCalculator.money(products.stockCost()));}
}
`);
java('controller/FinanceController',`package br.com.florir.controller;
import br.com.florir.dto.FinanceDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.service.FinanceService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/manager") public class FinanceController {
 private final FinanceService service;public FinanceController(FinanceService s){service=s;}
 @GetMapping("/finance") PageView<EntryView> entries(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.entries(page,size);}
 @PostMapping("/finance") @ResponseStatus(HttpStatus.CREATED) EntryView entry(@Valid @RequestBody EntryInput d){return service.entry(d);}
 @GetMapping("/inventory-movements") PageView<StockView> stock(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.stock(page,size);}
 @GetMapping("/audit") PageView<AuditView> audit(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.audit(page,size);}
 @GetMapping("/reports/summary") Summary summary(){return service.summary();}
}
`);
java('dto/RelationshipDtos',`package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.time.*;
public final class RelationshipDtos {
 public record BenefitInput(@NotBlank String customerId,String couponId,@Min(1900) @Max(3000) int year){}
 public record BenefitView(String id,String customerId,String customerName,String couponId,int year,Instant grantedAt){}
 public record GiftInput(@NotBlank String customerId,@NotBlank @Size(max=255) String description,@NotNull LocalDate date){}
 public record GiftView(String id,String customerId,String customerName,String description,LocalDate date){}
 public record BillingInput(@NotBlank @Size(max=4000) String message){}
 public record BillingView(String id,String installmentId,String message,Instant declaredSentAt){}
 public record WhatsAppPreview(String installmentId,String message,String url,boolean sent){}
 public record TicketInput(String orderId,@NotBlank @Size(max=255) String subject,@NotBlank @Size(max=4000) String message){}
 public record ReplyInput(@NotBlank @Size(max=4000) String reply,@NotBlank @Pattern(regexp="OPEN|RESOLVED") String status){}
 public record TicketView(String id,String customerId,String orderId,String subject,String message,String status,String reply,Instant createdAt){}
}
`);
java('service/RelationshipService',`package br.com.florir.service;
import br.com.florir.dto.RelationshipDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.model.*;import br.com.florir.repository.*;import br.com.florir.security.ActorService;import br.com.florir.exception.ApiException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.time.*;import java.util.*;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;
@Service @Transactional public class RelationshipService {
 private final BirthdayBenefitRepository benefits;private final GiftRepository gifts;private final BillingRecordRepository billing;private final InstallmentRepository installments;private final CustomerRepository customers;private final CouponRepository coupons;private final SupportTicketRepository tickets;private final PurchaseOrderRepository orders;private final ActorService actor;private final AuditService audit;
 public RelationshipService(BirthdayBenefitRepository b,GiftRepository g,BillingRecordRepository br,InstallmentRepository i,CustomerRepository c,CouponRepository co,SupportTicketRepository t,PurchaseOrderRepository o,ActorService a,AuditService au){benefits=b;gifts=g;billing=br;installments=i;customers=c;coupons=co;tickets=t;orders=o;actor=a;audit=au;}
 private BenefitView view(BirthdayBenefit b){return new BenefitView(b.getId().toString(),b.getCustomer().getId().toString(),b.getCustomer().getName(),b.getCoupon()==null?null:b.getCoupon().getId().toString(),b.getYear(),b.getGrantedAt());}
 private GiftView view(Gift g){return new GiftView(g.getId().toString(),g.getCustomer().getId().toString(),g.getCustomer().getName(),g.getDescription(),g.getDate());}
 private TicketView view(SupportTicket t){return new TicketView(t.getId().toString(),t.getCustomer().getId().toString(),t.getOrder()==null?null:t.getOrder().getId().toString(),t.getSubject(),t.getMessage(),t.getStatus(),t.getReply(),t.getCreatedAt());}
 @Transactional(readOnly=true) public PageView<BenefitView> benefits(int page,int size){return PageView.from(benefits.findAll(CatalogService.page(page,size)).map(this::view));}
 public BenefitView benefit(BenefitInput d){var c=customers.findById(CatalogService.id(d.customerId())).orElseThrow(ApiException::missing);if(c.getBirthday()==null)throw new ApiException(400,"Cliente sem aniversário cadastrado");var b=new BirthdayBenefit();b.setCustomer(c);b.setCoupon(d.couponId()==null?null:coupons.findById(CatalogService.id(d.couponId())).orElseThrow(ApiException::missing));b.setYear(d.year());b.setGrantedAt(Instant.now());benefits.save(b);audit.record("GRANT","BirthdayBenefit",b.getId());return view(b);}
 @Transactional(readOnly=true) public PageView<GiftView> gifts(int page,int size){return PageView.from(gifts.findAll(CatalogService.page(page,size)).map(this::view));}
 public GiftView gift(GiftInput d){var g=new Gift();g.setCustomer(customers.findById(CatalogService.id(d.customerId())).orElseThrow(ApiException::missing));g.setDescription(d.description());g.setDate(d.date());gifts.save(g);audit.record("GRANT","Gift",g.getId());return view(g);}
 @Transactional(readOnly=true) public WhatsAppPreview preview(String id){var i=installments.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);var c=i.getOrder().getCustomer();String message="Olá, "+c.getName()+"! A parcela "+i.getNumber()+" de R$ "+i.getAmount()+" vence em "+i.getDueDate()+".";return new WhatsAppPreview(i.getId().toString(),message,"https://wa.me/55"+c.getPhone()+"?text="+URLEncoder.encode(message,StandardCharsets.UTF_8),false);}
 public BillingView declareSent(String id,BillingInput d){var i=installments.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);if(!"PENDING".equals(i.getStatus()))throw ApiException.conflict("Parcela não está aberta");var b=new BillingRecord();b.setInstallment(i);b.setMessage(d.message());b.setDeclaredSentAt(Instant.now());billing.save(b);audit.record("DECLARE_SENT","BillingRecord",b.getId());return new BillingView(b.getId().toString(),i.getId().toString(),b.getMessage(),b.getDeclaredSentAt());}
 @Transactional(readOnly=true) public PageView<BillingView> billings(int page,int size){return PageView.from(billing.findAll(CatalogService.page(page,size)).map(b->new BillingView(b.getId().toString(),b.getInstallment().getId().toString(),b.getMessage(),b.getDeclaredSentAt())));}
 @Transactional(readOnly=true) public PageView<TicketView> tickets(int page,int size){return PageView.from(tickets.findAll(CatalogService.page(page,size)).map(this::view));}
 @Transactional(readOnly=true) public List<TicketView> myTickets(){return tickets.findByCustomerId(actor.customer().getId()).stream().map(this::view).toList();}
 public TicketView ticket(TicketInput d){var t=new SupportTicket();t.setCustomer(actor.customer());if(d.orderId()!=null){var o=orders.findById(CatalogService.id(d.orderId())).orElseThrow(ApiException::missing);actor.owns(o.getCustomer());t.setOrder(o);}t.setSubject(d.subject());t.setMessage(d.message());tickets.save(t);return view(t);}
 public TicketView reply(String id,ReplyInput d){var t=tickets.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);t.setReply(d.reply());t.setStatus(d.status());audit.record("REPLY","SupportTicket",t.getId());return view(t);}
}
`);
java('controller/RelationshipController',`package br.com.florir.controller;
import br.com.florir.dto.RelationshipDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.service.RelationshipService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;import java.util.List;
@RestController @RequestMapping("/api/v1") public class RelationshipController {
 private final RelationshipService service;public RelationshipController(RelationshipService s){service=s;}
 @GetMapping("/manager/birthday-benefits") PageView<BenefitView> benefits(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.benefits(page,size);}
 @PostMapping("/manager/birthday-benefits") @ResponseStatus(HttpStatus.CREATED) BenefitView benefit(@Valid @RequestBody BenefitInput d){return service.benefit(d);}
 @GetMapping("/manager/gifts") PageView<GiftView> gifts(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.gifts(page,size);}
 @PostMapping("/manager/gifts") @ResponseStatus(HttpStatus.CREATED) GiftView gift(@Valid @RequestBody GiftInput d){return service.gift(d);}
 @GetMapping("/manager/installments/{id}/whatsapp-preview") WhatsAppPreview preview(@PathVariable String id){return service.preview(id);}
 @PostMapping("/manager/installments/{id}/declare-sent") @ResponseStatus(HttpStatus.CREATED) BillingView declareSent(@PathVariable String id,@Valid @RequestBody BillingInput d){return service.declareSent(id,d);}
 @GetMapping("/manager/billings") PageView<BillingView> billings(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.billings(page,size);}
 @GetMapping("/manager/tickets") PageView<TicketView> tickets(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.tickets(page,size);}
 @PutMapping("/manager/tickets/{id}/reply") TicketView reply(@PathVariable String id,@Valid @RequestBody ReplyInput d){return service.reply(id,d);}
 @GetMapping("/customer/tickets") List<TicketView> mine(){return service.myTickets();}
 @PostMapping("/customer/tickets") @ResponseStatus(HttpStatus.CREATED) TicketView ticket(@Valid @RequestBody TicketInput d){return service.ticket(d);}
}
`);
