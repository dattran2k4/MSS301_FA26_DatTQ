package com.fudn.product_service.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

	private @Id String id;
	private String name;
	private String description;
	private BigDecimal price;
}
