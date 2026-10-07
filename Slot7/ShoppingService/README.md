# Shopping Service – Slot 7

Bài Part 3–4 tiếp tục từ `Slot6/ShoppingService`: Order Service gọi Inventory qua OpenFeign, Gateway định tuyến ba API và kiểm tra JWT do Keycloak cấp. Yêu cầu và test case chi tiết nằm trong `../Part3-4.md` và `../part3-4_Test.md`.

## Thành phần

| Thành phần | Port | Phiên bản |
|---|---:|---|
| Product Service | 8080 | Spring Boot 3.5.14 |
| Order Service | 8081 | Spring Boot 4.1.0, Spring Cloud 2025.1.3 |
| Inventory Service | 8082 | Spring Boot 3.5.14 |
| API Gateway | 9000 | Spring Boot 4.1.0, Spring Cloud 2025.1.3 |
| Keycloak | 8181 | Keycloak 24.0.1 |

Java 21 và Maven 3.9+ được dùng để build. Product và Inventory giữ phiên bản từ Slot 6; mã Java của Product được đặt về Java 21 để cả bài dùng chung JDK.

## Chạy local

Từ từng thư mục service, chạy các lệnh sau (mỗi service cần một terminal riêng):

```bash
cd order-service && docker compose up -d
cd ../product-service && docker compose up -d mongodb
cd ../api-gateway && docker compose up -d
```

`product-service/docker-compose.yml` có Mongo Express tùy chọn trên port 8083. Keycloak tự import realm `spring-microservices-realm` và client `spring-microservices-client` khi database còn mới. Secret mẫu trong file realm chỉ dành cho môi trường học tập local.

Sau khi MySQL, MongoDB và Keycloak sẵn sàng, chạy `mvn spring-boot:run` trong từng thư mục `product-service`, `inventory-service`, `order-service`, `api-gateway`. Gateway chỉ cho phép `/actuator/health` không cần token; các API khác phải có Bearer token.

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

Nếu realm đã tồn tại trong Keycloak, import lúc khởi động sẽ không ghi đè realm đó. Xem `Part3-4.md` mục 4.5 để cấu hình client bằng giao diện khi cần.

## Kiểm thử

```bash
cd order-service && mvn test       # 2 test, cần Docker cho MySQL Testcontainers
cd ../api-gateway && mvn test      # 5 test, không cần Docker/Keycloak thật
```

Test Gateway dùng WireMock cho ba service đích. Test Order dùng WireMock cho Inventory và MySQL Testcontainers để xác nhận đơn hết hàng không được lưu.
