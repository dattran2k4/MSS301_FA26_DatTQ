package com.mss301.brand.repository;
import com.mss301.brand.entity.SagaOperation;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface SagaOperationRepository extends JpaRepository<SagaOperation,String> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from SagaOperation s where s.sagaId = :id")
 Optional<SagaOperation> lockById(String id);
}
