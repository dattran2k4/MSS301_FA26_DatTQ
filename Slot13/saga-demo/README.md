# Saga demo — project skeletons

Ba project Maven độc lập theo Step 1 của `../Saga_guide.md`:

| Project | Package | Port | Database dự kiến |
|---|---|---|---|
| inventory-service | com.mss301.inventory | 8082 | MongoDB |
| payment-service | com.mss301.payment | 8083 | SQL Server |
| order-service | com.mss301.order | 8081 | MySQL |

- Java 21, Spring Boot 4.1.1 theo guideline.
- Trong IntelliJ, mở `pom.xml` của từng service dưới dạng project Maven, chọn Project SDK 21 và Load Maven Changes.
- Mỗi service có Maven Wrapper (`mvnw`, `mvnw.cmd`). Có thể build trong thư mục service bằng `./mvnw -DskipTests package`.
- Hiện mới có dependencies, main class và tên/cổng ứng dụng. Đã bổ sung hạ tầng database TODO-0; chưa triển khai nghiệp vụ Saga.
- Cần bổ sung database và cấu hình kết nối ở các bước tiếp theo trước khi chạy đầy đủ ứng dụng; các service JPA chưa thể khởi động khi chưa cấu hình datasource.

## TODO-0 — chạy database

Từ thư mục `Slot13/saga-demo`:

```bash
docker compose up -d --wait mysql sqlserver mongodb
docker compose ps
```

Cả 3 container phải có trạng thái `healthy`. SQL Server dùng `linux/amd64` để chạy trên máy Apple Silicon.

| Database | Địa chỉ từ máy host | Database name | Tài khoản |
|---|---|---|---|
| MySQL | localhost:3307 | orderdb | root / root |
| SQL Server | localhost:14330 | paymentdb | sa / Saga@12345 |
| MongoDB | localhost:27017 | inventorydb | Không bật authentication trong demo |

MongoDB chạy replica set `rs0` một node. Dùng URI `mongodb://localhost:27017/inventorydb?directConnection=true` từ máy host. Database MongoDB được tạo khi ứng dụng ghi dữ liệu đầu tiên.

Healthcheck SQL Server tự tạo `paymentdb` nếu chưa tồn tại. Cổng database chỉ mở trên loopback của máy host. Các named volume giữ dữ liệu khi dừng hoặc tạo lại container.

Dừng database và giữ dữ liệu: `docker compose down`. Lệnh `docker compose down -v` xóa cả dữ liệu của bài demo, chỉ dùng khi muốn reset.

### Trạng thái kiểm tra trên máy hiện tại

- MySQL: `healthy`, đã xác nhận có `orderdb`.
- MongoDB: `healthy`, replica set `rs0` đã là PRIMARY.
- SQL Server: thoát với lỗi `Unable to create a new asynchronous I/O context`, errno 38 (`Function not implemented`). TODO-0 chưa đạt đủ 3 database healthy.
- OrbStack hiện có `rosetta: false`. Bước xử lý tiếp theo là thử bật Rosetta và khởi động lại OrbStack, rồi chạy lại Compose. Việc khởi động lại có thể gián đoạn các container khác, gồm `cinema-mysql` đang chạy; cần thống nhất trước khi thực hiện.
- Tài liệu OrbStack về Rosetta: https://docs.orbstack.dev/settings
