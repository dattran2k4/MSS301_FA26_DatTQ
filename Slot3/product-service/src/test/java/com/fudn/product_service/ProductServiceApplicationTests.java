package com.fudn.product_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fudn.product_service.dto.ProductRequest;
import com.fudn.product_service.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void cleanup() {
		productRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void shouldCreateProduct() throws Exception {
		ProductRequest productRequest = new ProductRequest(
				"Test Product",
				"This is a test product",
				BigDecimal.valueOf(19.99));

		mockMvc.perform(post("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(productRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.name").value("Test Product"))
				.andExpect(jsonPath("$.description").value("This is a test product"))
				.andExpect(jsonPath("$.price").value(19.99));

		assertThat(productRepository.findAll()).hasSize(1);
	}

	@Test
	void shouldGetAllProducts() throws Exception {
		ProductRequest productRequest = new ProductRequest(
				"MongoDB Course",
				"Microservice product item",
				BigDecimal.valueOf(29.99));

		mockMvc.perform(post("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(productRequest)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/product"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").isNotEmpty())
				.andExpect(jsonPath("$[0].name").value("MongoDB Course"))
				.andExpect(jsonPath("$[0].description").value("Microservice product item"))
				.andExpect(jsonPath("$[0].price").value(29.99));
	}
}
