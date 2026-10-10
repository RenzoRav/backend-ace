import fs from 'node:fs';import path from 'node:path';const put=(p,s)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};const java=(p,s)=>put('src/main/java/br/com/florir/'+p+'.java',s);
java('dto/CatalogDtos',`package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.List;
public final class CatalogDtos {
 public record NameInput(@NotBlank @Size(max=255) String name){}
 public record Named(String id,String name){}
 public record ProductInput(@NotBlank @Size(max=255) String name,@Size(max=4000) String description,@Size(max=4000) String imageUrl,@NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal price,@NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal cost,@Min(0) int stock,@Min(0) int minimumStock,@NotBlank String brandId,@NotBlank String categoryId,boolean active){}
 public record ProductView(String id,String name,String description,String imageUrl,BigDecimal price,BigDecimal cost,int stock,int minimumStock,boolean active,Named brand,Named category){}
 public record PublicProduct(String id,String name,String description,String imageUrl,BigDecimal price,int stock,Named brand,Named category){}
 public record PageView<T>(List<T> content,int page,int size,long totalElements,int totalPages) {public static <T> PageView<T> from(org.springframework.data.domain.Page<T> p){return new PageView<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}}
 public record StockAdjustment(@Min(0) int stock,@NotBlank @Size(max=4000) String reason){}
}
`);
java('dto/CustomerDtos',`package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.time.LocalDate;
public final class CustomerDtos {
 public record CustomerInput(@NotBlank @Size(max=255) String name,@NotBlank String phone,LocalDate birthday){}
 public record CustomerView(String id,String name,String phone,LocalDate birthday){}
 public record AddressInput(@NotBlank String label,@NotBlank @Pattern(regexp="[0-9]{8}") String postalCode,@NotBlank String street,@NotBlank String number,String complement,@NotBlank String district,@NotBlank String city,@NotBlank @Pattern(regexp="[A-Z]{2}") String state){}
 public record AddressView(String id,String label,String postalCode,String street,String number,String complement,String district,String city,String state){}
}
`);
java('service/AuditService',`package br.com.florir.service;
import br.com.florir.repository.AuditLogRepository;import br.com.florir.model.AuditLog;import br.com.florir.security.ActorService;import org.springframework.stereotype.Service;
@Service public class AuditService {
 private final AuditLogRepository repository;private final ActorService actor;
 public AuditService(AuditLogRepository r,ActorService a){repository=r;actor=a;}
 public void record(String action,String entity,Long id){if(!"MANAGER".equals(actor.role()))return;var log=new AuditLog();log.setManager(actor.manager());log.setAction(action);log.setEntity(entity);log.setEntityId(id.toString());repository.save(log);}
}
`);
java('service/CatalogService',`package br.com.florir.service;
import br.com.florir.dto.CatalogDtos.*;import br.com.florir.model.*;import br.com.florir.repository.*;import br.com.florir.exception.ApiException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.data.domain.*;import java.util.*;
@Service @Transactional public class CatalogService {
 private final ProductRepository products;private final BrandRepository brands;private final CategoryRepository categories;private final InventoryMovementRepository movements;private final AuditService audit;
 public CatalogService(ProductRepository p,BrandRepository b,CategoryRepository c,InventoryMovementRepository m,AuditService a){products=p;brands=b;categories=c;movements=m;audit=a;}
 public static Long id(String id){try {long value=Long.parseLong(id);if(value<1)throw new NumberFormatException();return value;}catch(NumberFormatException e){throw new ApiException(400,"Identificador inválido");}}
 public static Pageable page(int page,int size){if(page<0||size<1||size>100)throw new ApiException(400,"Paginação inválida");return PageRequest.of(page,size,Sort.by("id"));}
 public ProductView view(Product p){return new ProductView(p.getId().toString(),p.getName(),p.getDescription(),p.getImageUrl(),p.getPrice(),p.getCost(),p.getStock(),p.getMinimumStock(),p.getActive(),new Named(p.getBrand().getId().toString(),p.getBrand().getName()),new Named(p.getCategory().getId().toString(),p.getCategory().getName()));}
 public PublicProduct publicView(Product p){return new PublicProduct(p.getId().toString(),p.getName(),p.getDescription(),p.getImageUrl(),p.getPrice(),p.getStock(),new Named(p.getBrand().getId().toString(),p.getBrand().getName()),new Named(p.getCategory().getId().toString(),p.getCategory().getName()));}
 @Transactional(readOnly=true) public PageView<ProductView> list(int page,int size){return PageView.from(products.findAll(page(page,size)).map(this::view));}
 @Transactional(readOnly=true) public PageView<PublicProduct> catalog(int page,int size){return PageView.from(products.findByActiveTrue(page(page,size)).map(this::publicView));}
 @Transactional(readOnly=true) public ProductView get(String id){return view(products.findById(id(id)).orElseThrow(ApiException::missing));}
 public ProductView save(String id,ProductInput data){var p=id==null?new Product():products.lockById(id(id)).orElseThrow(ApiException::missing);int old=p.getStock();p.setName(data.name().strip());p.setDescription(data.description());p.setImageUrl(data.imageUrl());p.setPrice(PricingCalculator.money(data.price()));p.setCost(PricingCalculator.money(data.cost()));p.setMinimumStock(data.minimumStock());p.setActive(data.active());p.setBrand(brands.findById(id(data.brandId())).orElseThrow(ApiException::missing));p.setCategory(categories.findById(id(data.categoryId())).orElseThrow(ApiException::missing));p.setStock(data.stock());products.save(p);stockMovement(p,data.stock()-old,id==null?"INITIAL":"ADJUSTMENT","Cadastro/edição pela gestora");audit.record(id==null?"CREATE":"UPDATE","Product",p.getId());return view(p);}
 public ProductView adjust(String id,StockAdjustment data){var p=products.lockById(id(id)).orElseThrow(ApiException::missing);int delta=data.stock()-p.getStock();p.setStock(data.stock());stockMovement(p,delta,"ADJUSTMENT",data.reason());audit.record("ADJUST_STOCK","Product",p.getId());return view(p);}
 private void stockMovement(Product p,int delta,String type,String reason){if(delta==0)return;var movement=new InventoryMovement();movement.setProduct(p);movement.setType(type);movement.setQuantity(delta);movement.setDescription(reason);movements.save(movement);}
 public void deactivate(String id){var p=products.lockById(id(id)).orElseThrow(ApiException::missing);p.setActive(false);audit.record("DEACTIVATE","Product",p.getId());}
 @Transactional(readOnly=true) public List<Named> brands(){return brands.findAll(Sort.by("name")).stream().map(b->new Named(b.getId().toString(),b.getName())).toList();}
 @Transactional(readOnly=true) public List<Named> categories(){return categories.findAll(Sort.by("name")).stream().map(c->new Named(c.getId().toString(),c.getName())).toList();}
 public Named brand(String id,NameInput data){var b=id==null?new Brand():brands.findById(id(id)).orElseThrow(ApiException::missing);if((id==null||!b.getName().equalsIgnoreCase(data.name().strip()))&&brands.existsByNameIgnoreCase(data.name().strip()))throw ApiException.conflict("Marca já cadastrada");b.setName(data.name().strip());brands.save(b);audit.record(id==null?"CREATE":"UPDATE","Brand",b.getId());return new Named(b.getId().toString(),b.getName());}
 public Named category(String id,NameInput data){var c=id==null?new Category():categories.findById(id(id)).orElseThrow(ApiException::missing);if((id==null||!c.getName().equalsIgnoreCase(data.name().strip()))&&categories.existsByNameIgnoreCase(data.name().strip()))throw ApiException.conflict("Categoria já cadastrada");c.setName(data.name().strip());categories.save(c);audit.record(id==null?"CREATE":"UPDATE","Category",c.getId());return new Named(c.getId().toString(),c.getName());}
 public void deleteBrand(String id){var b=brands.findById(id(id)).orElseThrow(ApiException::missing);audit.record("DELETE","Brand",b.getId());brands.delete(b);}
 public void deleteCategory(String id){var c=categories.findById(id(id)).orElseThrow(ApiException::missing);audit.record("DELETE","Category",c.getId());categories.delete(c);}
}
`);
java('controller/CatalogController',`package br.com.florir.controller;
import br.com.florir.dto.CatalogDtos.*;import br.com.florir.service.CatalogService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;import java.util.List;
@RestController @RequestMapping("/api/v1") public class CatalogController {
 private final CatalogService service;public CatalogController(CatalogService s){service=s;}
 @GetMapping("/catalog/products") PageView<PublicProduct> catalog(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.catalog(page,size);}
 @GetMapping({"/catalog/brands","/manager/brands"}) List<Named> brands(){return service.brands();}
 @GetMapping({"/catalog/categories","/manager/categories"}) List<Named> categories(){return service.categories();}
 @GetMapping("/manager/products") PageView<ProductView> products(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(page,size);}
 @GetMapping("/manager/products/{id}") ProductView product(@PathVariable String id){return service.get(id);}
 @PostMapping("/manager/products") @ResponseStatus(HttpStatus.CREATED) ProductView create(@Valid @RequestBody ProductInput data){return service.save(null,data);}
 @PutMapping("/manager/products/{id}") ProductView update(@PathVariable String id,@Valid @RequestBody ProductInput data){return service.save(id,data);}
 @PostMapping("/manager/products/{id}/stock") ProductView stock(@PathVariable String id,@Valid @RequestBody StockAdjustment data){return service.adjust(id,data);}
 @DeleteMapping("/manager/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deactivate(@PathVariable String id){service.deactivate(id);}
 @PostMapping("/manager/brands") @ResponseStatus(HttpStatus.CREATED) Named brand(@Valid @RequestBody NameInput data){return service.brand(null,data);}
 @PutMapping("/manager/brands/{id}") Named brand(@PathVariable String id,@Valid @RequestBody NameInput data){return service.brand(id,data);}
 @DeleteMapping("/manager/brands/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteBrand(@PathVariable String id){service.deleteBrand(id);}
 @PostMapping("/manager/categories") @ResponseStatus(HttpStatus.CREATED) Named category(@Valid @RequestBody NameInput data){return service.category(null,data);}
 @PutMapping("/manager/categories/{id}") Named category(@PathVariable String id,@Valid @RequestBody NameInput data){return service.category(id,data);}
 @DeleteMapping("/manager/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCategory(@PathVariable String id){service.deleteCategory(id);}
}
`);
java('service/CustomerService',`package br.com.florir.service;
import br.com.florir.dto.CustomerDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.model.*;import br.com.florir.repository.*;import br.com.florir.security.ActorService;import br.com.florir.exception.ApiException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service @Transactional public class CustomerService {
 private final CustomerRepository customers;private final CustomerAddressRepository addresses;private final ActorService actor;private final AuditService audit;
 public CustomerService(CustomerRepository c,CustomerAddressRepository a,ActorService actor,AuditService audit){customers=c;addresses=a;this.actor=actor;this.audit=audit;}
 public CustomerView view(Customer c){return new CustomerView(c.getId().toString(),c.getName(),c.getPhone(),c.getBirthday());}
 public AddressView view(CustomerAddress a){return new AddressView(a.getId().toString(),a.getLabel(),a.getPostalCode(),a.getStreet(),a.getNumber(),a.getComplement(),a.getDistrict(),a.getCity(),a.getState());}
 @Transactional(readOnly=true) public PageView<CustomerView> list(int page,int size){return PageView.from(customers.findAll(CatalogService.page(page,size)).map(this::view));}
 @Transactional(readOnly=true) public CustomerView me(){return view(actor.customer());}
 @Transactional(readOnly=true) public CustomerView get(String id){var c=customers.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);actor.owns(c);return view(c);}
 public CustomerView save(String id,CustomerInput data){var c=id==null?new Customer():customers.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);if(id!=null)actor.owns(c);String phone=PhoneNumbers.normalize(data.phone());if((id==null||!phone.equals(c.getPhone()))&&customers.existsByPhone(phone))throw ApiException.conflict("Telefone já cadastrado");c.setName(data.name().strip());c.setPhone(phone);c.setBirthday(data.birthday());customers.save(c);audit.record(id==null?"CREATE":"UPDATE","Customer",c.getId());return view(c);}
 public CustomerView updateMe(CustomerInput data){return save(actor.customer().getId().toString(),data);}
 @Transactional(readOnly=true) public java.util.List<AddressView> addresses(){return addresses.findByCustomerId(actor.customer().getId()).stream().map(this::view).toList();}
 public AddressView address(String id,AddressInput d){var c=actor.customer();var a=id==null?new CustomerAddress():addresses.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);if(id!=null)actor.owns(a.getCustomer());a.setCustomer(c);a.setLabel(d.label());a.setPostalCode(d.postalCode());a.setStreet(d.street());a.setNumber(d.number());a.setComplement(d.complement());a.setDistrict(d.district());a.setCity(d.city());a.setState(d.state());addresses.save(a);return view(a);}
 public void deleteAddress(String id){var a=addresses.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);actor.owns(a.getCustomer());addresses.delete(a);}
}
`);
java('controller/CustomerController',`package br.com.florir.controller;
import br.com.florir.dto.CustomerDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.service.CustomerService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;import java.util.List;
@RestController @RequestMapping("/api/v1") public class CustomerController {
 private final CustomerService service;public CustomerController(CustomerService s){service=s;}
 @GetMapping("/manager/customers") PageView<CustomerView> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(page,size);}
 @GetMapping("/manager/customers/{id}") CustomerView get(@PathVariable String id){return service.get(id);}
 @PostMapping("/manager/customers") @ResponseStatus(HttpStatus.CREATED) CustomerView create(@Valid @RequestBody CustomerInput data){return service.save(null,data);}
 @PutMapping("/manager/customers/{id}") CustomerView update(@PathVariable String id,@Valid @RequestBody CustomerInput data){return service.save(id,data);}
 @GetMapping("/customer/me") CustomerView me(){return service.me();}
 @PutMapping("/customer/me") CustomerView me(@Valid @RequestBody CustomerInput data){return service.updateMe(data);}
 @GetMapping("/customer/addresses") List<AddressView> addresses(){return service.addresses();}
 @PostMapping("/customer/addresses") @ResponseStatus(HttpStatus.CREATED) AddressView createAddress(@Valid @RequestBody AddressInput data){return service.address(null,data);}
 @PutMapping("/customer/addresses/{id}") AddressView updateAddress(@PathVariable String id,@Valid @RequestBody AddressInput data){return service.address(id,data);}
 @DeleteMapping("/customer/addresses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable String id){service.deleteAddress(id);}
}
`);
put('src/main/resources/db/migration/V2__normalized_catalog_names.sql','CREATE UNIQUE INDEX uq_brand_lower_name ON brand(lower(name));\nCREATE UNIQUE INDEX uq_category_lower_name ON category(lower(name));\n');
