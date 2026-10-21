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
- Hiện mới có dependencies, main class và tên/cổng ứng dụng. Chưa triển khai TODO-0 hay nghiệp vụ Saga.
- Cần bổ sung database và cấu hình kết nối ở các bước tiếp theo trước khi chạy đầy đủ ứng dụng; các service JPA chưa thể khởi động khi chưa cấu hình datasource.
