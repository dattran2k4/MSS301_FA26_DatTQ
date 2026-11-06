package com.mss301.watch.repository;
import com.mss301.watch.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SagaStateRepository extends JpaRepository<SagaState,Long> { boolean existsByStatusNotIn(java.util.Collection<com.mss301.watch.service.saga.SagaStatus> statuses); java.util.Optional<SagaState> findBySagaId(String sagaId); java.util.List<SagaState> findAllByOrderByIdDesc(org.springframework.data.domain.Pageable pageable); }
