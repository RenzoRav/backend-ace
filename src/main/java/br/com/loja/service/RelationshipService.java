package br.com.loja.service;

import br.com.loja.dto.CatalogDtos.PageView;
import br.com.loja.dto.RelationshipDtos.*;
import br.com.loja.exception.ApiException;
import br.com.loja.model.*;
import br.com.loja.repository.*;
import br.com.loja.security.ActorService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RelationshipService {
  private final BirthdayBenefitRepository birthdayBenefitRepository;
  private final GiftRepository giftRepository;
  private final BillingRecordRepository billingRecordRepository;
  private final InstallmentRepository installmentRepository;
  private final CustomerRepository customerRepository;
  private final CouponRepository couponRepository;
  private final SupportTicketRepository supportTicketRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final ActorService actorService;
  private final AuditService auditService;

  private BenefitView view(BirthdayBenefit birthdayBenefit) {
    return new BenefitView(
        birthdayBenefit.getId().toString(),
        birthdayBenefit.getCustomer().getId().toString(),
        birthdayBenefit.getCustomer().getName(),
        birthdayBenefit.getCoupon() == null ? null : birthdayBenefit.getCoupon().getId().toString(),
        birthdayBenefit.getYear(),
        birthdayBenefit.getGrantedAt());
  }

  private GiftView view(Gift gift) {
    return new GiftView(
        gift.getId().toString(),
        gift.getCustomer().getId().toString(),
        gift.getCustomer().getName(),
        gift.getDescription(),
        gift.getDate());
  }

  private TicketView view(SupportTicket supportTicket) {
    return new TicketView(
        supportTicket.getId().toString(),
        supportTicket.getCustomer().getId().toString(),
        supportTicket.getOrder() == null ? null : supportTicket.getOrder().getId().toString(),
        supportTicket.getSubject(),
        supportTicket.getMessage(),
        supportTicket.getStatus(),
        supportTicket.getReply(),
        supportTicket.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public PageView<BenefitView> benefits(int page, int size) {
    return PageView.from(
        birthdayBenefitRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  public BenefitView benefit(BenefitInput benefitInput) {
    var customer =
        customerRepository
            .findById(CatalogService.id(benefitInput.customerId()))
            .orElseThrow(ApiException::missing);
    if (customer.getBirthday() == null)
      throw new ApiException(400, "Cliente sem aniversário cadastrado");
    var birthdayBenefit = new BirthdayBenefit();
    birthdayBenefit.setCustomer(customer);
    birthdayBenefit.setCoupon(
        benefitInput.couponId() == null
            ? null
            : couponRepository
                .findById(CatalogService.id(benefitInput.couponId()))
                .orElseThrow(ApiException::missing));
    birthdayBenefit.setYear(benefitInput.year());
    birthdayBenefit.setGrantedAt(Instant.now());
    birthdayBenefitRepository.save(birthdayBenefit);
    auditService.record("GRANT", "BirthdayBenefit", birthdayBenefit.getId());
    return view(birthdayBenefit);
  }

  @Transactional(readOnly = true)
  public PageView<GiftView> gifts(int page, int size) {
    return PageView.from(giftRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  public GiftView gift(GiftInput giftInput) {
    var gift = new Gift();
    gift.setCustomer(
        customerRepository
            .findById(CatalogService.id(giftInput.customerId()))
            .orElseThrow(ApiException::missing));
    gift.setDescription(giftInput.description());
    gift.setDate(giftInput.date());
    giftRepository.save(gift);
    auditService.record("GRANT", "Gift", gift.getId());
    return view(gift);
  }

  @Transactional(readOnly = true)
  public WhatsAppPreview preview(String id) {
    var installment =
        installmentRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    var customer = installment.getOrder().getCustomer();
    String message =
        "Olá, "
            + customer.getName()
            + "! A parcela "
            + installment.getNumber()
            + " de R$ "
            + installment.getAmount()
            + " vence em "
            + installment.getDueDate()
            + ".";
    return new WhatsAppPreview(
        installment.getId().toString(),
        message,
        "https://wa.me/55"
            + customer.getPhone()
            + "?text="
            + URLEncoder.encode(message, StandardCharsets.UTF_8),
        false);
  }

  public BillingView declareSent(String id, BillingInput billingInput) {
    var installment =
        installmentRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    if (!"PENDING".equals(installment.getStatus()))
      throw ApiException.conflict("Parcela não está aberta");
    var billingRecord = new BillingRecord();
    billingRecord.setInstallment(installment);
    billingRecord.setMessage(billingInput.message());
    billingRecord.setDeclaredSentAt(Instant.now());
    billingRecordRepository.save(billingRecord);
    auditService.record("DECLARE_SENT", "BillingRecord", billingRecord.getId());
    return new BillingView(
        billingRecord.getId().toString(),
        installment.getId().toString(),
        billingRecord.getMessage(),
        billingRecord.getDeclaredSentAt());
  }

  @Transactional(readOnly = true)
  public PageView<BillingView> billings(int page, int size) {
    return PageView.from(
        billingRecordRepository
            .findAll(CatalogService.page(page, size))
            .map(
                billingRecord ->
                    new BillingView(
                        billingRecord.getId().toString(),
                        billingRecord.getInstallment().getId().toString(),
                        billingRecord.getMessage(),
                        billingRecord.getDeclaredSentAt())));
  }

  @Transactional(readOnly = true)
  public PageView<TicketView> tickets(int page, int size) {
    return PageView.from(
        supportTicketRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public List<TicketView> myTickets() {
    return supportTicketRepository.findByCustomerId(actorService.customer().getId()).stream()
        .map(this::view)
        .toList();
  }

  public TicketView ticket(TicketInput ticketInput) {
    var supportTicket = new SupportTicket();
    supportTicket.setCustomer(actorService.customer());
    if (ticketInput.orderId() != null) {
      var purchaseOrder =
          purchaseOrderRepository
              .findById(CatalogService.id(ticketInput.orderId()))
              .orElseThrow(ApiException::missing);
      actorService.owns(purchaseOrder.getCustomer());
      supportTicket.setOrder(purchaseOrder);
    }
    supportTicket.setSubject(ticketInput.subject());
    supportTicket.setMessage(ticketInput.message());
    supportTicketRepository.save(supportTicket);
    return view(supportTicket);
  }

  public TicketView reply(String id, ReplyInput replyInput) {
    var supportTicket =
        supportTicketRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    supportTicket.setReply(replyInput.reply());
    supportTicket.setStatus(replyInput.status());
    auditService.record("REPLY", "SupportTicket", supportTicket.getId());
    return view(supportTicket);
  }

  @Transactional(readOnly = true)
  public WhatsAppPreview birthdayPreview(String id) {
    var customer =
        customerRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    if (customer.getBirthday() == null)
      throw new ApiException(400, "Cliente sem aniversário cadastrado");
    String message =
        "Feliz aniversário, " + customer.getName() + "! A Loja deseja um dia especial para você.";
    return new WhatsAppPreview(
        null,
        message,
        "https://wa.me/55"
            + customer.getPhone()
            + "?text="
            + URLEncoder.encode(message, StandardCharsets.UTF_8),
        false);
  }
}
