package br.com.florir.controller;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.RelationshipDtos.*;
import br.com.florir.service.RelationshipService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RelationshipController {
  private final RelationshipService service;

  public RelationshipController(RelationshipService s) {
    service = s;
  }

  @GetMapping("/manager/birthday-benefits")
  PageView<BenefitView> benefits(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.benefits(page, size);
  }

  @PostMapping("/manager/birthday-benefits")
  @ResponseStatus(HttpStatus.CREATED)
  BenefitView benefit(@Valid @RequestBody BenefitInput d) {
    return service.benefit(d);
  }

  @GetMapping("/manager/gifts")
  PageView<GiftView> gifts(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.gifts(page, size);
  }

  @PostMapping("/manager/gifts")
  @ResponseStatus(HttpStatus.CREATED)
  GiftView gift(@Valid @RequestBody GiftInput d) {
    return service.gift(d);
  }

  @GetMapping("/manager/installments/{id}/whatsapp-preview")
  WhatsAppPreview preview(@PathVariable String id) {
    return service.preview(id);
  }

  @PostMapping("/manager/installments/{id}/declare-sent")
  @ResponseStatus(HttpStatus.CREATED)
  BillingView declareSent(@PathVariable String id, @Valid @RequestBody BillingInput d) {
    return service.declareSent(id, d);
  }

  @GetMapping("/manager/billings")
  PageView<BillingView> billings(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.billings(page, size);
  }

  @GetMapping("/manager/tickets")
  PageView<TicketView> tickets(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.tickets(page, size);
  }

  @PutMapping("/manager/tickets/{id}/reply")
  TicketView reply(@PathVariable String id, @Valid @RequestBody ReplyInput d) {
    return service.reply(id, d);
  }

  @GetMapping("/customer/tickets")
  List<TicketView> mine() {
    return service.myTickets();
  }

  @PostMapping("/customer/tickets")
  @ResponseStatus(HttpStatus.CREATED)
  TicketView ticket(@Valid @RequestBody TicketInput d) {
    return service.ticket(d);
  }

  @GetMapping("/manager/customers/{id}/birthday-whatsapp-preview")
  WhatsAppPreview birthday(@PathVariable String id) {
    return service.birthdayPreview(id);
  }
}
