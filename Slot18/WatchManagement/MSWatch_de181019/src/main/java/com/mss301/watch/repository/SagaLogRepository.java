package com.mss301.watch.repository;
import com.mss301.watch.entity.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SagaLogRepository extends JpaRepository<SagaLog,Long> { java.util.List<SagaLog> findBySagaIdOrderByIdAsc(String sagaId); }
