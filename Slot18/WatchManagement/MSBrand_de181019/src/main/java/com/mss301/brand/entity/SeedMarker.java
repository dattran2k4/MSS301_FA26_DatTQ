package com.mss301.brand.entity;
import jakarta.persistence.*;
@Entity @Table(name="SeedHistory") public class SeedMarker {
 @Id @Column(length=80) private String id;
 protected SeedMarker() {} public SeedMarker(String id) { this.id=id; }
}
