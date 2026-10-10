import fs from 'node:fs';import path from 'node:path';
const put=(p,s)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};
const snake=s=>s.replace(/[A-Z]/g,c=>'_'+c.toLowerCase());
const entities={
 Brand:[['String','name','required unique']], Category:[['String','name','required unique']],
 Customer:[['String','name','required'],['String','phone','required unique'],['LocalDate','birthday','']],
 CustomerAccount:[['@Customer','customer','required unique'],['String','email','required unique'],['String','passwordHash','required'],['long','credentialVersion','required']],
 Manager:[['String','name','required'],['String','email','required unique'],['String','passwordHash','required'],['long','credentialVersion','required'],['boolean','active','required','true']],
 Product:[['@Brand','brand','required'],['@Category','category','required'],['String','name','required'],['String','description','long'],['String','imageUrl','long'],['BigDecimal','price','required'],['BigDecimal','cost','required'],['int','stock','required'],['int','minimumStock','required'],['boolean','active','required','true']],
 Coupon:[['String','code','required unique'],['String','type','required'],['BigDecimal','value','required'],['LocalDate','validFrom','required'],['LocalDate','validUntil','required'],['Integer','usageLimit','']],
 Promotion:[['String','name','required'],['BigDecimal','percent','required'],['LocalDate','validFrom','required'],['LocalDate','validUntil','required']],
 ProductPromotion:[['@Product','product','required'],['@Promotion','promotion','required']],
 InterestRate:[['int','installmentCount','required unique'],['BigDecimal','percent','required']],
 PurchaseOrder:[['@Customer','customer','required'],['@Coupon','coupon',''],['@InterestRate','interestRate',''],['String','status','required','"NEW"'],['String','paymentStatus','required','"PENDING"'],['String','paymentMethod','required'],['String','stockEffect','required','"NONE"'],['String','origin','required'],['BigDecimal','subtotal','required'],['BigDecimal','discount','required'],['BigDecimal','shipping','required'],['BigDecimal','interest','required'],['BigDecimal','total','required'],['int','installmentCount','required','1'],['boolean','delivery','required'],['String','addressSnapshot','long'],['String','idempotencyKey','unique'],['String','requestHash',''],['String','customerName','required'],['String','customerPhone','required']],
 OrderItem:[['@PurchaseOrder','order','required'],['@Product','product','required'],['int','quantity','required'],['BigDecimal','unitPrice','required'],['BigDecimal','unitCost','required'],['String','productName','required'],['String','brandName','required'],['String','categoryName','required']],
 Installment:[['@PurchaseOrder','order','required'],['int','number','required'],['BigDecimal','amount','required'],['LocalDate','dueDate','required'],['String','status','required','"PENDING"'],['Instant','paidAt','']],
 BillingRecord:[['@Installment','installment','required'],['String','message','required long'],['Instant','declaredSentAt','required']],
 InventoryMovement:[['@Product','product','required'],['@PurchaseOrder','order',''],['String','type','required'],['int','quantity','required'],['String','description','long']],
 FinancialMovement:[['@PurchaseOrder','order',''],['@Installment','installment',''],['String','type','required'],['String','description','required'],['BigDecimal','amount','required']],
 BirthdayBenefit:[['@Customer','customer','required'],['@Coupon','coupon',''],['int','year','required'],['Instant','grantedAt','required']],
 Gift:[['@Customer','customer','required'],['String','description','required'],['LocalDate','date','required']],
 AuditLog:[['@Manager','manager','required'],['String','action','required'],['String','entity','required'],['String','entityId','required']],
 StatusHistory:[['@PurchaseOrder','order','required'],['@Manager','manager',''],['String','previousStatus',''],['String','nextStatus','required']],
 CustomerAddress:[['@Customer','customer','required'],['String','label','required'],['String','postalCode','required'],['String','street','required'],['String','number','required'],['String','complement',''],['String','district','required'],['String','city','required'],['String','state','required']],
 SupportTicket:[['@Customer','customer','required'],['@PurchaseOrder','order',''],['String','subject','required'],['String','message','required long'],['String','status','required','"OPEN"'],['String','reply','long']],
 RefreshSession:[['String','role','required'],['Long','accountId','required'],['String','tokenHash','required unique'],['Instant','expiresAt','required'],['boolean','revoked','required']],
};
put('src/main/java/br/com/florir/model/BaseEntity.java',`package br.com.florir.model;
import jakarta.persistence.*;import java.time.Instant;
@MappedSuperclass
public abstract class BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;}public long getVersion(){return version;}public Instant getCreatedAt(){return createdAt;}
}
`);
let sql='-- PostgreSQL: schema inicial Florir. Valores monetários são NUMERIC.\n';
for(const [name,fields] of Object.entries(entities)){
 let body=`package br.com.florir.model;\nimport jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;\n@Entity @Table(name="${snake(name).slice(1)}"${name==='ProductPromotion'?',uniqueConstraints=@UniqueConstraint(columnNames={"product_id","promotion_id"})':name==='Installment'?',uniqueConstraints=@UniqueConstraint(columnNames={"order_id","number"})':name==='BirthdayBenefit'?',uniqueConstraints=@UniqueConstraint(columnNames={"customer_id","year"})':''})\npublic class ${name} extends BaseEntity {\n`;
 let cols=['id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY','version BIGINT NOT NULL DEFAULT 0','created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP'];
 for(const [raw,key,flags='',init] of fields){const ref=raw.startsWith('@');const type=ref?raw.slice(1):raw;const col=snake(key)+(ref?'_id':''); const required=flags.includes('required'),unique=flags.includes('unique');
  body+=ref?` @ManyToOne(fetch=FetchType.LAZY${required?',optional=false':''}) @JoinColumn(name="${col}"${required?',nullable=false':''}${unique?',unique=true':''})\n`:` @Column(name="${col}"${required?',nullable=false':''}${unique?',unique=true':''}${flags.includes('long')?',length=4000':''}${type==='BigDecimal'?',precision=14,scale=2':''})\n`;
  body+=` private ${type} ${key}${init?'='+init:''};\n public ${type} get${key[0].toUpperCase()+key.slice(1)}(){return ${key};}\n public void set${key[0].toUpperCase()+key.slice(1)}(${type} value){this.${key}=value;}\n`;
  const st=ref?'BIGINT':({String:flags.includes('long')?'VARCHAR(4000)':'VARCHAR(255)',long:'BIGINT',Long:'BIGINT',int:'INTEGER',Integer:'INTEGER',boolean:'BOOLEAN',BigDecimal:'NUMERIC(14,2)',LocalDate:'DATE',Instant:'TIMESTAMPTZ'})[type];
  cols.push(`${col} ${st}${required?' NOT NULL':''}${unique?' UNIQUE':''}${ref?' REFERENCES '+snake(type).slice(1)+'(id)':''}`);
 }
 body+='}\n';put(`src/main/java/br/com/florir/model/${name}.java`,body);
 if(name==='ProductPromotion')cols.push('UNIQUE(product_id,promotion_id)');if(name==='Installment')cols.push('UNIQUE(order_id,number)');if(name==='BirthdayBenefit')cols.push('UNIQUE(customer_id,year)');
 if(name==='Product')cols.push('CHECK(price >= 0 AND cost >= 0 AND stock >= 0 AND minimum_stock >= 0)');if(name==='Coupon')cols.push("CHECK(type IN ('FIXED','PERCENT') AND value >= 0 AND (type <> 'PERCENT' OR value <= 100) AND valid_until >= valid_from AND (usage_limit IS NULL OR usage_limit > 0))");if(name==='Promotion')cols.push('CHECK(percent BETWEEN 0 AND 100 AND valid_until >= valid_from)');if(name==='InterestRate')cols.push('CHECK(installment_count BETWEEN 1 AND 12 AND percent >= 0)');if(name==='OrderItem')cols.push('CHECK(quantity > 0 AND unit_price >= 0 AND unit_cost >= 0)');if(name==='PurchaseOrder')cols.push("CHECK(total >= 0 AND installment_count BETWEEN 1 AND 12 AND status IN ('NEW','CONFIRMED','PREPARING','FINISHED','CANCELLED'))");if(name==='Installment')cols.push("CHECK(amount > 0 AND number > 0 AND status IN ('PENDING','PAID','CANCELLED'))");if(name==='FinancialMovement')cols.push("CHECK(amount > 0 AND type IN ('IN','OUT'))");
 sql+=`CREATE TABLE ${snake(name).slice(1)} (\n  ${cols.join(',\n  ')}\n);\n\n`;
 let methods='';
 if(['Brand','Category'].includes(name))methods=` boolean existsByNameIgnoreCase(String name);`;
 if(['Manager','CustomerAccount'].includes(name))methods=` java.util.Optional<${name}> findByEmail(String email); boolean existsByEmail(String email);`;
 if(name==='Customer')methods=' boolean existsByPhone(String phone);';
 if(['Product','PurchaseOrder','Installment','Coupon','RefreshSession'].includes(name))methods+=`\n @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select e from ${name} e where e.id = :id") java.util.Optional<${name}> lockById(@org.springframework.data.repository.query.Param("id") Long id);`;
 if(name==='Product')methods+='\n org.springframework.data.domain.Page<Product> findByActiveTrue(org.springframework.data.domain.Pageable pageable);';
 if(name==='PurchaseOrder')methods+='\n java.util.List<PurchaseOrder> findByCustomerIdOrderByCreatedAtDesc(Long id); java.util.Optional<PurchaseOrder> findByIdempotencyKey(String key); long countByCouponIdAndStatusNot(Long id,String status);';
 if(name==='OrderItem')methods+='\n java.util.List<OrderItem> findByOrderIdOrderById(Long id);';
 if(name==='Installment')methods+='\n java.util.List<Installment> findByOrderIdOrderByNumber(Long id);';
 if(name==='FinancialMovement')methods+='\n java.util.List<FinancialMovement> findByOrderId(Long id); boolean existsByInstallmentIdAndType(Long id,String type);';
 if(name==='ProductPromotion')methods+='\n java.util.List<ProductPromotion> findByProductId(Long id);';
 if(name==='Coupon')methods+='\n java.util.Optional<Coupon> findByCode(String code);';
 if(name==='InterestRate')methods+='\n java.util.Optional<InterestRate> findByInstallmentCount(int count);';
 if(name==='CustomerAddress')methods+='\n java.util.List<CustomerAddress> findByCustomerId(Long id);';
 if(name==='SupportTicket')methods+='\n java.util.List<SupportTicket> findByCustomerId(Long id);';
 if(name==='StatusHistory')methods+='\n java.util.List<StatusHistory> findByOrderIdOrderByCreatedAt(Long id);';
 if(name==='RefreshSession')methods+='\n @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) java.util.Optional<RefreshSession> findByTokenHash(String hash); java.util.List<RefreshSession> findByRoleAndAccountId(String role,Long id);';
 put(`src/main/java/br/com/florir/repository/${name}Repository.java`,`package br.com.florir.repository;\nimport br.com.florir.model.${name};import org.springframework.data.jpa.repository.*;\npublic interface ${name}Repository extends JpaRepository<${name},Long> {${methods}\n}\n`);
}
sql+='CREATE INDEX idx_order_customer ON purchase_order(customer_id);\nCREATE INDEX idx_order_item_order ON order_item(order_id);\nCREATE INDEX idx_inventory_product ON inventory_movement(product_id);\nCREATE INDEX idx_financial_order ON financial_movement(order_id);\nCREATE INDEX idx_installment_due ON installment(status,due_date);\n';
put('src/main/resources/db/migration/V1__florir_schema.sql',sql);
put('src/main/resources/application.properties',`spring.application.name=BackendFlorir\nspring.profiles.default=dev\nserver.port=\${PORT:8080}\nspring.datasource.url=\${DB_URL:jdbc:postgresql://127.0.0.1:5433/florir}\nspring.datasource.username=\${DB_USER:florir}\nspring.datasource.password=\${DB_PASSWORD}\nspring.jpa.hibernate.ddl-auto=validate\nspring.jpa.open-in-view=false\nspring.flyway.enabled=true\nserver.error.include-message=never\nflorir.allowed-origin=\${FRONTEND_ORIGIN:http://localhost:3000}\nflorir.bootstrap.enabled=\${BOOTSTRAP_ENABLED:false}\nflorir.bootstrap.email=\${MANAGER_EMAIL:}\nflorir.bootstrap.password=\${MANAGER_PASSWORD:}\nflorir.jwt.issuer=florir-api\nflorir.jwt.audience=florir-frontend\nflorir.jwt.secret=\${JWT_SECRET}\nflorir.cookie.secure=true\n`);
put('src/main/resources/application-dev.properties',`spring.datasource.password=\${DB_PASSWORD:florir_dev}\nflorir.bootstrap.enabled=\${BOOTSTRAP_ENABLED:true}\nflorir.bootstrap.email=\${MANAGER_EMAIL:gestora@florir.local}\nflorir.bootstrap.password=\${MANAGER_PASSWORD:FlorirDev123!}\nflorir.jwt.secret=\${JWT_SECRET:florir-development-key-change-in-production-2026-at-least-32-bytes}\nflorir.cookie.secure=false\n`);
put('src/test/resources/application-test.properties',`spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/florir_test\nspring.datasource.username=florir\nspring.datasource.password=florir_dev\nflorir.jwt.secret=florir-test-only-key-longer-than-thirty-two-bytes\nflorir.cookie.secure=false\nflorir.bootstrap.enabled=true\nflorir.bootstrap.email=gestora@florir.local\nflorir.bootstrap.password=FlorirDev123!\n`);
put('src/test/java/br/com/florir/BackendFlorirApplicationTests.java',`package br.com.florir;\nimport org.junit.jupiter.api.Test;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;\n@SpringBootTest @ActiveProfiles("test") class BackendFlorirApplicationTests { @Test void contextLoads(){} }\n`);
