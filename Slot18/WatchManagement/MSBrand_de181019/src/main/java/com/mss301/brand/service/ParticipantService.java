package com.mss301.brand.service;
import com.mss301.brand.entity.SagaOperation;
import com.mss301.brand.repository.SagaOperationRepository;
import com.mss301.brand.dto.AssignmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service @RequiredArgsConstructor
public class ParticipantService {
 private final SagaOperationRepository operations;
 private final BrandService brands;
 @Transactional
 public AssignmentResponse apply(String sagaId,String phase,Integer watchId,Integer brandId) {
  try { java.util.UUID.fromString(sagaId); } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Saga ID"); }
  if (!"EXECUTE".equals(phase) && !"COMPENSATE".equals(phase)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Saga phase");
  var existing=operations.lockById(sagaId);
  var op=existing.orElseGet(() -> operations.saveAndFlush(new SagaOperation(sagaId,watchId)));
  if (!watchId.equals(op.getWatchId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Saga belongs to another watch");
  if (op.isCompensated() || (existing.isPresent() && "EXECUTE".equals(phase))) return new AssignmentResponse(watchId,brandId,null,"ALREADY_APPLIED");
  AssignmentResponse result=brandId==null?brands.unassignWatch(watchId):brands.assignWatch(brandId,watchId);
  op.setCompensated("COMPENSATE".equals(phase));
  return result;
 }
}
