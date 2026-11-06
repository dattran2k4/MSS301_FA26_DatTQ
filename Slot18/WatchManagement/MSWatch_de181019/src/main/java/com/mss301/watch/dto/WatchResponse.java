package com.mss301.watch.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data @Builder public class WatchResponse {
 private Integer watchId, categoryId, brandId, stock;
 private String name, categoryName, caseMaterial, strapMaterial, waterResistance;
 private BigDecimal price; private LocalDate releaseDate;
}
