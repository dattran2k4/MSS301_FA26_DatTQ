package com.mss301.brand.dto;
import lombok.*;
@Data @Builder public class BrandResponse { private Integer brandId; private String brandName, countryOfOrigin; private long watchCount; }
