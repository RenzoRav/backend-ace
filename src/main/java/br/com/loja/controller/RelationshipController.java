package br.com.loja.controller;

import br.com.loja.dto.CatalogDtos.PageView;
import br.com.loja.dto.RelationshipDtos.*;
import br.com.loja.service.RelationshipService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RelationshipController {
  private final RelationshipService relationshipService;

  @GetMapping("/manager/birthday-benefits")
  PageView<BenefitView> benefits(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return relationshipService.benefits(page, size);
  }

  @PostMapping("/manager/birthday-benefits")
  @ResponseStatus(HttpStatus.CREATED)
  BenefitView benefit(@Valid @RequestBody BenefitInput benefitInput) {
    return relationshipService.benefit(benefitInput);
  }

  @GetMapping("/manager/gifts")
  PageView<GiftView> gifts(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return relationshipService.gifts(page, size);
  }

  @PostMapping("/manager/gifts")
  @ResponseStatus(HttpStatus.CREATED)
  GiftView gift(@Valid @RequestBody GiftInput giftInput) {
    return relationshipService.gift(giftInput);
  }

  @GetMapping("/manager/installments/{id}/whatsapp-preview")
  WhatsAppPreview preview(@PathVariable String id) {
    return relationshipService.preview(id);
  }

  @PostMapping("/manager/installments/{id}/declare-sent")
  @ResponseStatus(HttpStatus.CREATED)
  BillingView declareSent(@PathVariable String id, @Valid @RequestBody BillingInput billingInput) {
    return relationshipService.declareSent(id, billingInput);
  }

  @GetMapping("/manager/billings")
  PageView<BillingView> billings(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return relationshipService.billings(page, size);
  }

  @GetMapping("/manager/tickets")
  PageView<TicketView> tickets(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return relationshipService.tickets(page, size);
  }

  @PutMapping("/manager/tickets/{id}/reply")
  TicketView reply(@PathVariable String id, @Valid @RequestBody ReplyInput replyInput) {
    return relationshipService.reply(id, replyInput);
  }

  @GetMapping("/customer/tickets/paged")
  PageView<TicketView> minePage(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return relationshipService.myTicketsPage(page, size);
  }

  @GetMapping("/customer/tickets")
  List<TicketView> mine() {
    return relationshipService.myTickets();
  }

  @PostMapping("/customer/tickets")
  @ResponseStatus(HttpStatus.CREATED)
  TicketView ticket(@Valid @RequestBody TicketInput ticketInput) {
    return relationshipService.ticket(ticketInput);
  }

  @GetMapping("/manager/customers/{id}/birthday-whatsapp-preview")
  WhatsAppPreview birthday(@PathVariable String id) {
    return relationshipService.birthdayPreview(id);
  }
}
