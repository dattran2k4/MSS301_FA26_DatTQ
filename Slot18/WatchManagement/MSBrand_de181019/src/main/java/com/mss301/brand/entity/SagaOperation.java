package com.mss301.brand.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="SagaOperations") @Data @NoArgsConstructor
public class SagaOperation {
 @Id @Column(length=36) private String sagaId;
 private Integer watchId;
 private boolean compensated;
 public SagaOperation(String sagaId,Integer watchId) { this.sagaId=sagaId; this.watchId=watchId; }
}
