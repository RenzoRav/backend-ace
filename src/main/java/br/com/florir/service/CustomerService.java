package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.CustomerDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import br.com.florir.security.ActorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {
  private final CustomerRepository customers;
  private final CustomerAddressRepository addresses;
  private final ActorService actor;
  private final AuditService audit;

  public CustomerService(
      CustomerRepository c, CustomerAddressRepository a, ActorService actor, AuditService audit) {
    customers = c;
    addresses = a;
    this.actor = actor;
    this.audit = audit;
  }

  public CustomerView view(Customer c) {
    return new CustomerView(c.getId().toString(), c.getName(), c.getPhone(), c.getBirthday());
  }

  public AddressView view(CustomerAddress a) {
    return new AddressView(
        a.getId().toString(),
        a.getLabel(),
        a.getPostalCode(),
        a.getStreet(),
        a.getNumber(),
        a.getComplement(),
        a.getDistrict(),
        a.getCity(),
        a.getState());
  }

  @Transactional(readOnly = true)
  public PageView<CustomerView> list(int page, int size) {
    return PageView.from(customers.findAll(CatalogService.page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public CustomerView me() {
    return view(actor.customer());
  }

  @Transactional(readOnly = true)
  public CustomerView get(String id) {
    var c = customers.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actor.owns(c);
    return view(c);
  }

  public CustomerView save(String id, CustomerInput data) {
    var c =
        id == null
            ? new Customer()
            : customers.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    if (id != null) actor.owns(c);
    String phone = PhoneNumbers.normalize(data.phone());
    if ((id == null || !phone.equals(c.getPhone())) && customers.existsByPhone(phone))
      throw ApiException.conflict("Telefone já cadastrado");
    c.setName(data.name().strip());
    c.setPhone(phone);
    c.setBirthday(data.birthday());
    customers.save(c);
    audit.record(id == null ? "CREATE" : "UPDATE", "Customer", c.getId());
    return view(c);
  }

  public CustomerView updateMe(CustomerInput data) {
    return save(actor.customer().getId().toString(), data);
  }

  @Transactional(readOnly = true)
  public java.util.List<AddressView> addresses() {
    return addresses.findByCustomerId(actor.customer().getId()).stream().map(this::view).toList();
  }

  public AddressView address(String id, AddressInput d) {
    var c = actor.customer();
    var a =
        id == null
            ? new CustomerAddress()
            : addresses.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    if (id != null) actor.owns(a.getCustomer());
    a.setCustomer(c);
    a.setLabel(d.label());
    a.setPostalCode(d.postalCode());
    a.setStreet(d.street());
    a.setNumber(d.number());
    a.setComplement(d.complement());
    a.setDistrict(d.district());
    a.setCity(d.city());
    a.setState(d.state());
    addresses.save(a);
    return view(a);
  }

  public void deleteAddress(String id) {
    var a = addresses.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actor.owns(a.getCustomer());
    addresses.delete(a);
  }
}
