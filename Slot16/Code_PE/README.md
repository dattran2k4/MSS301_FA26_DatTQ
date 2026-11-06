# DE181019 — MSS301 Media Booking

Bài thực hành gồm ba ứng dụng độc lập, theo `DeThiSample_New.docx` và công thức trong `HuongDanSample_New.docx`.

| Project | Package | Cổng | Công nghệ |
| --- | --- | --- | --- |
| DE181019UserService | fu.de181019.user | 8081 | MVC, JPA, SQL Server |
| DE181019PostService | fu.de181019.post | 8082 | MVC, JPA, SQL Server, OpenFeign |
| DE181019MediaGateway | fu.de181019.gateway | 8080 | Spring Cloud Gateway WebFlux |

JDK 21, Maven, Spring Boot 4.1.0, Spring Cloud 2025.1.2, springdoc 3.0.3. Không dùng Lombok. Mỗi service có pom riêng; pom ngoài cùng chỉ gom ba module để build một lần.

## Chạy ứng dụng

1. Mở `Code_PE/pom.xml` bằng IntelliJ, chọn Project SDK 21 và Load Maven.
2. Chuẩn bị SQL Server 2016+ tại `localhost:1433`, bật SQL Authentication, tài khoản `sa` / mật khẩu `sa` theo đề. Nếu SQL Server áp dụng password policy, quản trị viên cần cấu hình tài khoản phù hợp trước khi chạy. Project không tự sửa tài khoản hoặc database đang có.
3. Chạy SQL lần lượt trong SSMS/Azure Data Studio hoặc SQL client có hỗ trợ `GO`:
   - `database/00_database.sql`
   - `database/01_users.sql`
   - `database/02_posts.sql`
   - `database/03_seed.sql` (dữ liệu mẫu, chạy một lần).
4. Trong thư mục `Code_PE`, build:

```bash
mvn clean verify
```

5. Mở ba terminal, chạy lần lượt:

```bash
java -jar DE181019UserService/target/DE181019UserService-1.0.0.jar
java -jar DE181019PostService/target/DE181019PostService-1.0.0.jar
java -jar DE181019MediaGateway/target/DE181019MediaGateway-1.0.0.jar
```

Hoặc chạy từng class `DE181019...Application` trong IntelliJ. Không cần Eureka, Config Server hay Docker cho ứng dụng.

Swagger Gateway: http://localhost:8080/swagger-ui.html — chọn user-service hoặc post-service. Swagger riêng: http://localhost:8081/swagger-ui.html và http://localhost:8082/swagger-ui.html. Cả hai MVC service có CorsFilter để trình duyệt tải tài liệu qua cổng khác; Gateway có CorsWebFilter. Gọi nghiệp vụ qua `http://localhost:8080/api/...`.

## Chức năng và quy ước

- User: POST tạo; PUT cập nhật một phần; GET chi tiết; DELETE chuyển DEACTIVATED; GET danh sách lọc role/status, phân trang.
- Post: POST tạo; PUT cập nhật một phần và tính lại tiền theo giá hiện tại; GET chi tiết có object user; DELETE chuyển CANCELLED; GET danh sách lọc advertiserName (chuỗi con)/status và phân trang.
- Trường bỏ qua hoặc null trong PUT giữ giá trị hiện tại. Các giá trị được gộp rồi kiểm tra toàn bộ ràng buộc. Dùng Integer trong DTO để phân biệt bỏ qua với số 0; JSON vẫn là số nguyên theo đề.
- ID do database sinh; totalCost do server tính, không lấy từ client.
- Mặc định user ACTIVE, post SCHEDULED khi không gửi status. Enum gửi lên phải đúng chữ hoa.
- Chỉ nhận ngày dạng `dd/MM/yyyy`, kiểm tra ngày thực và endDate > startDate. Ngày JSON dùng UTC để tránh lệch ngày theo múi giờ máy.
- `totalCost = ratePerPost × (endDate − startDate)`, không nhân numberOfPosts. Ví dụ 25/07→28/07, giá 500000 ⇒ 1500000, theo hướng dẫn mẫu.
- Khi tạo chiến dịch hoặc chuyển sang user khác: user phải tồn tại và ACTIVE. Khi sửa chiến dịch của cùng user, vẫn kiểm tra user tồn tại, maxDailyPosts và giá hiện tại; đề không cấm sửa chiến dịch cũ khi user bị ngưng hoạt động.
- numberOfPosts phải từ 1 đến maxDailyPosts của user; đề không yêu cầu cộng dồn giữa nhiều chiến dịch.
- page bắt đầu 0; size mặc định 10, từ 1 đến 100. Filter enum sai trả 406.
- Lỗi Feign 404 trả 404/status 4; lỗi kết nối hoặc server từ UserService trả 500/status 0, không giả thành “user không tồn tại”.
- Entity map đúng tên bảng/cột SQL, enum lưu dạng STRING, `ddl-auto=none` trong cấu hình chạy chính. Hai bảng dùng chung database và FK theo SQL đề.

| HTTP | status trong JSON | Ý nghĩa |
| --- | --- | --- |
| 200/201 | 1 | Thành công |
| 406 | 2 | Validation |
| 226 | 3 | Trùng username |
| 404 | 4 | Không tìm thấy |
| 400 | 5 | User không ACTIVE |
| 500 | 0 | Lỗi server |

Response luôn gồm status, message, data, timestamp ISO 8601 UTC. DELETE thành công và các lỗi trả data=null.

## Kiểm thử

`mvn verify` chạy integration test qua HTTP trên cổng ngẫu nhiên:

- User: CRUD, trùng username, partial update, validation, filter/phân trang, soft delete, Swagger và CORS.
- Post: CRUD, format ngày, ngày không hợp lệ, tính lại tiền, maxDailyPosts, filter, Feign với HTTP server giả lập, user không ACTIVE/404/lỗi upstream, hủy và Swagger.
- Gateway: chuyển tiếp cả collection/detail của hai route, preflight CORS, Swagger UI và cấu hình tổng hợp docs.

Test dùng H2 trong bộ nhớ và `create-drop` chỉ trong test. Không thay đổi cấu hình SQL Server chạy chính. Chưa xác minh trực tiếp với SQL Server trong môi trường hiện tại; cần chạy SQL và thử `requests.http` với SQL Server trước khi nộp. Không khẳng định H2 thay thế được kiểm tra đặc thù SQL Server.

Mở `requests.http` bằng IntelliJ HTTP Client hoặc copy JSON sang Swagger. Chỉnh ID theo response tạo mới.

## Nộp bài

Tại thư mục Code_PE:

```bash
mvn clean
cd ..
zip -r DE181019_MediaBooking.zip Code_PE -x '*/.idea/*' '*/.DS_Store' '*/target/*'
```

ZIP chứa source ba project, pom tổng và SQL. Nộp qua PEA. Không đưa target hoặc cache Maven vào bài nộp.
