package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.CustomerDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import br.com.florir.security.ActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {
  private final CustomerRepository customerRepository;
  private final CustomerAddressRepository customerAddressRepository;
  private final ActorService actorService;
  private final AuditService auditService;

  public CustomerView view(Customer customer) {
    return new CustomerView(
        customer.getId().toString(),
        customer.getName(),
        customer.getPhone(),
        customer.getBirthday());
  }

  public AddressView view(CustomerAddress customerAddress) {
    return new AddressView(
        customerAddress.getId().toString(),
        customerAddress.getLabel(),
        customerAddress.getPostalCode(),
        customerAddress.getStreet(),
        customerAddress.getNumber(),
        customerAddress.getComplement(),
        customerAddress.getDistrict(),
        customerAddress.getCity(),
        customerAddress.getState());
  }

  @Transactional(readOnly = true)
  public PageView<CustomerView> list(int page, int size) {
    return PageView.from(
        customerRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public CustomerView me() {
    return view(actorService.customer());
  }

  @Transactional(readOnly = true)
  public CustomerView get(String id) {
    var customer =
        customerRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    actorService.owns(customer);
    return view(customer);
  }

  public CustomerView save(String id, CustomerInput data) {
    var customer =
        id == null
            ? new Customer()
            : customerRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    if (id != null) actorService.owns(customer);
    String phone = PhoneNumbers.normalize(data.phone());
    if ((id == null || !phone.equals(customer.getPhone()))
        && customerRepository.existsByPhone(phone))
      throw ApiException.conflict("Telefone já cadastrado");
    customer.setName(data.name().strip());
    customer.setPhone(phone);
    customer.setBirthday(data.birthday());
    customerRepository.save(customer);
    auditService.record(id == null ? "CREATE" : "UPDATE", "Customer", customer.getId());
    return view(customer);
  }

  public CustomerView updateMe(CustomerInput data) {
    return save(actorService.customer().getId().toString(), data);
  }

  @Transactional(readOnly = true)
  public java.util.List<AddressView> addresses() {
    return customerAddressRepository.findByCustomerId(actorService.customer().getId()).stream()
        .map(this::view)
        .toList();
  }

  public AddressView address(String id, AddressInput addressInput) {
    var customer = actorService.customer();
    var customerAddress =
        id == null
            ? new CustomerAddress()
            : customerAddressRepository
                .findById(CatalogService.id(id))
                .orElseThrow(ApiException::missing);
    if (id != null) actorService.owns(customerAddress.getCustomer());
    customerAddress.setCustomer(customer);
    customerAddress.setLabel(addressInput.label());
    customerAddress.setPostalCode(addressInput.postalCode());
    customerAddress.setStreet(addressInput.street());
    customerAddress.setNumber(addressInput.number());
    customerAddress.setComplement(addressInput.complement());
    customerAddress.setDistrict(addressInput.district());
    customerAddress.setCity(addressInput.city());
    customerAddress.setState(addressInput.state());
    customerAddressRepository.save(customerAddress);
    return view(customerAddress);
  }

  public void deleteAddress(String id) {
    var customerAddress =
        customerAddressRepository
            .findById(CatalogService.id(id))
            .orElseThrow(ApiException::missing);
    actorService.owns(customerAddress.getCustomer());
    customerAddressRepository.delete(customerAddress);
  }
}
