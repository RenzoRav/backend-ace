package br.com.loja.service;
import br.com.loja.model.SessionFamily;
import br.com.loja.repository.SessionFamilyRepository;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class SessionFamilyService {
 private final SessionFamilyRepository sessionFamilyRepository;
 @Transactional public SessionFamily create(String role,Long accountId,Instant now){
  var family=new SessionFamily();family.setId(UUID.randomUUID().toString());family.setRole(role);family.setAccountId(accountId);family.setCreatedAt(now);family.setAbsoluteExpiresAt(now.plus(Duration.ofDays(30)));return sessionFamilyRepository.save(family);
 }
 @Transactional(readOnly=true) public boolean isActive(String familyId,String role,Long accountId,Instant now){
  if(familyId==null)return false;
  return sessionFamilyRepository.findById(familyId).map(family->!family.getRevoked()&&role.equals(family.getRole())&&accountId.equals(family.getAccountId())&&family.getAbsoluteExpiresAt().isAfter(now)).orElse(false);
 }
}

