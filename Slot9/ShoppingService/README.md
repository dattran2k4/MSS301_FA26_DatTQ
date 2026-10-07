# Shopping Service – Slot 9

Bài Part 5 tiếp tục từ `Slot7/ShoppingService`: ba service cung cấp OpenAPI và Swagger UI, còn API Gateway hiển thị tài liệu của cả hệ thống. Xem `../Part5_swaggerdocs.md` và `../Part5_guide.md` để đối chiếu yêu cầu.

## Thành phần

| Thành phần | Port | Spring Boot | Springdoc |
|---|---:|---:|---:|
| Product Service | 8080 | 3.5.14 | 2.8.17 |
| Order Service | 8081 | 4.1.0 | 3.1.1 |
| Inventory Service | 8082 | 3.5.14 | 2.8.17 |
| API Gateway | 9000 | 4.1.0 | 3.1.1 |
| Keycloak | 8181 | — | — |

Cần Java 21, Maven 3.9+ và Docker. Hướng dẫn mẫu dùng Springdoc 2.5.0 và package `com.fudn.productservice`; project này dùng các phiên bản Spring Boot mới hơn và package Product thực tế là `com.fudn.product_service`. Cấu hình được đặt theo phiên bản và package đang chạy để Spring quét được bean OpenAPI.

## Chạy local

Từ thư mục `Slot9/ShoppingService`, khởi động hạ tầng:

```bash
(cd order-service && docker compose up -d)
(cd product-service && docker compose up -d mongodb)
(cd api-gateway && docker compose up -d)
```

Keycloak tự import realm `spring-microservices-realm` và client `spring-microservices-client` khi database còn mới. Secret trong file realm chỉ dùng để thực hành local.

Sau khi MongoDB, MySQL và Keycloak sẵn sàng, mở bốn terminal và chạy `mvn spring-boot:run` trong từng thư mục service.

| URL | Nội dung |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI Product |
| `http://localhost:8081/swagger-ui.html` | Swagger UI Order |
| `http://localhost:8082/swagger-ui.html` | Swagger UI Inventory |
| `http://localhost:9000/swagger-ui.html` | Swagger UI tổng hợp với ba service |
| `http://localhost:8080/api-docs` | OpenAPI JSON Product (tương tự port 8081, 8082) |

Gateway cho phép `/actuator/health`, Swagger UI, `/v3/api-docs/**` và `/aggregate/**` không cần JWT. Các API nghiệp vụ vẫn cần Bearer token. Các đường dẫn nghiệp vụ thực tế là `/api/products`, `/api/order` và `/api/inventory`.

Lấy token và gọi thử Gateway:

```bash
curl -s -X POST http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=spring-microservices-client \
  -d client_secret=mss301-dev-secret-change-me

curl -i -H 'Authorization: Bearer <access_token>' http://localhost:9000/api/products
curl -i -H 'Authorization: Bearer <access_token>' 'http://localhost:9000/api/inventory?skuCode=iphone_15&quantity=1'
curl -i -X POST -H 'Authorization: Bearer <access_token>' -H 'Content-Type: application/json' \
  -d '{"skuCode":"iphone_15","price":1000,"quantity":1}' http://localhost:9000/api/order
```

## Kiểm thử

Chạy `mvn test` trong mỗi thư mục `product-service`, `inventory-service`, `order-service`, `api-gateway`. Ba service đầu dùng Testcontainers nên cần Docker; test Gateway dùng WireMock, không cần Keycloak thật. Test Swagger xác nhận UI, metadata, endpoint trong JSON, route tổng hợp và quyền truy cập không có JWT.
