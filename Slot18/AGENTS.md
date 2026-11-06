# Hướng dẫn làm việc với Assignment 02

## Phạm vi và nguồn yêu cầu

- Xây dựng hệ thống Watch Management bằng Spring Boot Microservices, Saga Pattern và Database per Service cho MSS301, Slot18.
- Đề gốc: `MMS301_Assignment 02_Watch Management_Saga Pattern.docx`. Đọc đề trước khi triển khai hoặc thay đổi nghiệp vụ; các file `Assignment_Readme.md` và `Assignment_Solution.md` chỉ là tài liệu tham khảo, không thay thế đề gốc.
- Hướng dẫn này áp dụng cho toàn bộ thư mục Slot18. Chỉ dẫn trực tiếp của người dùng được ưu tiên; phân biệt yêu cầu của đề với quyết định triển khai.
- Người dùng đã chốt tên bảng thương hiệu là **`WatchBrands`** và trường chống nước là **`WaterResistance`**. Không dùng `WatchBrand` hoặc `WaterpMaterial` làm tên schema/API mới.
- Chưa có yêu cầu xây dựng giao diện web. Phạm vi kiểm thử client được đề nêu là Postman.

## Kiến trúc bắt buộc

| Project | Trách nhiệm | Database và bảng |
| --- | --- | --- |
| `MSAccount_StudentCode` | Tài khoản và đăng nhập | `DBAccount`: `StoreAccounts` |
| `MSBrand_StudentCode` | Thương hiệu đồng hồ | `DBBrand`: `WatchBrands` |
| `MSWatch_StudentCode` | Đồng hồ và danh mục | `DBWatch`: `Watches`, `WatchCategories` |
| `APIGateway_StudentCode` | Điểm truy cập chung và định tuyến request | Không sở hữu dữ liệu nghiệp vụ của các service |

- Mã sinh viên đã được người dùng xác nhận: `de181019`. Tên project dùng hậu tố `_de181019`.
- Dùng SQL Server, JPA/Hibernate và cách tiếp cận code-first với Java `@Entity` để tạo schema.
- Mỗi service chỉ kết nối database của chính mình. Không truy vấn, join hoặc ghi trực tiếp database của service khác.
- `Watches.CategoryID` là khóa ngoại nội bộ tới `WatchCategories.CategoryID`.
- `Watches.BrandID` tham chiếu thương hiệu thuộc Brand Service. Không tạo quan hệ JPA xuyên service hoặc khóa ngoại xuyên database; kiểm tra tham chiếu bằng giao tiếp giữa các service.
- Client và bộ kiểm thử nghiệp vụ Postman truy cập thông qua API Gateway. Giao tiếp nội bộ giữa các service được tách khỏi API dành cho client.

## Mô hình dữ liệu

Giữ đầy đủ các trường và giới hạn trong đề. Tên thuộc tính Java có thể dùng camelCase nhưng phải ánh xạ rõ tên bảng/cột chuẩn.

- `StoreAccounts`: `AccountID` (INT, PK), `Username` (VARCHAR(20), NOT NULL), `Email` (VARCHAR(100), NOT NULL), `Password` (VARCHAR(100), NOT NULL), `Role` (INT), `IsActive` (boolean, mặc định true; ánh xạ SQL Server phù hợp).
- `WatchCategories`: `CategoryID` (INT, PK), `CategoryName` (VARCHAR(100)), `Description` (TEXT), `MovementType` (VARCHAR(50)), `PriceRange` (VARCHAR(100)).
- `WatchBrands`: `BrandID` (INT, PK), `BrandName` (VARCHAR(200)), `CountryOfOrigin` (VARCHAR(50)).
- `Watches`: `WatchID` (INT, PK), `Name` (VARCHAR(255)), `CategoryID` (INT), `BrandID` (INT), `CaseMaterial` (VARCHAR(100)), `StrapMaterial` (VARCHAR(100)), `WaterResistance` (VARCHAR(50)), `Price` (DECIMAL(10,2)), `ReleaseDate` (DATE), `Stock` (INT).
- Dùng kiểu số thập phân chính xác cho giá tiền và kiểu ngày không chứa giờ cho `ReleaseDate`.

## Dữ liệu mẫu

- Nhập đầy đủ, đúng dữ liệu từ mục Data của đề: 5 tài khoản, 5 danh mục, 5 thương hiệu và 10 đồng hồ; giữ ID và quan hệ tham chiếu.
- Vai trò: Administrator = 1, Staff = 2, Member = 3. Giữ đúng trạng thái `IsActive` của từng tài khoản.
- Mật khẩu đăng nhập mẫu là `@1`. Có thể lưu dạng băm để bảo vệ mật khẩu, nhưng phải giữ khả năng đăng nhập bằng giá trị mẫu; không trả mật khẩu hoặc giá trị băm qua API.
- Không tự sửa dữ liệu mẫu vì thấy không hợp lý về nghiệp vụ. Ví dụ, bản ghi IWC trong đề có `BrandID = 4`.
- Validation thêm/sửa không dùng để loại bỏ hoặc sửa dữ liệu seed: đề có đồng hồ giá dưới 100, tên ngắn và ngày phát hành trong quá khứ.
- Seed phải chạy lại an toàn, không nhân đôi bản ghi hoặc ghi đè dữ liệu đã được người dùng chỉnh sửa mỗi lần khởi động.

## Chức năng và phân quyền

### Xem danh sách

- Cho phép truy cập công khai, không yêu cầu đăng nhập.
- Trả toàn bộ thông tin của `Watches` và bổ sung `CategoryName` từ `WatchCategories`.
- Bản ghi vừa thêm phải xuất hiện ở đầu danh sách; dùng thứ tự sắp xếp xác định, không dựa vào thứ tự trả về mặc định của database.

### Thêm, sửa và xóa

- Chỉ cho phép tài khoản đã đăng nhập với vai trò Administrator thực hiện.
- Kiểm tra quyền phía server; không chỉ dựa vào Postman, client hoặc dữ liệu role do client tự gửi.
- Thêm và sửa phải kiểm tra: mọi trường nghiệp vụ bắt buộc có giá trị; `Price >= 100`; `Name` dài hơn 10 ký tự; `ReleaseDate` bằng ngày hiện tại.
- ID do hệ thống sinh không bắt buộc client nhập khi thêm. Giá trị số 0 không được coi là thiếu chỉ vì là falsy; các giới hạn bổ sung phải được ghi nhận là quyết định triển khai.
- Thống nhất múi giờ dùng để xác định ngày hiện tại và ghi rõ trong tài liệu chạy/kiểm thử.
- Thêm, sửa, xóa phải xử lý cập nhật ở các microservice liên quan bằng Saga theo thiết kế được ghi nhận.
- Tài khoản không hoạt động cần bị từ chối đăng nhập: đây là quyết định triển khai dựa trên ý nghĩa `IsActive`, không phải điều kiện được mô tả riêng trong mục CRUD của đề.

## Saga và những điểm cần quyết định

Đề Word chưa mô tả chi tiết tác động liên service. Tài liệu đi kèm `Assignment_Readme.md` mục 1.2 và 2.3 đã xác định `BrandWatches` là danh sách đồng hồ của từng thương hiệu, MSWatch điều phối Saga và MSBrand là participant. Dùng thiết kế này cho triển khai; chi tiết và giới hạn nằm trong `WatchManagement/docs/SAGA_DESIGN.md`.

- Trước khi code Saga, ghi thiết kế trong tài liệu riêng: service tham gia, thay đổi dữ liệu có ý nghĩa nghiệp vụ, thứ tự bước, trạng thái thành công/thất bại, hành động bù trừ và cách kiểm thử.
- Không coi một lời gọi kiểm tra thương hiệu tồn tại là toàn bộ Saga. Phải thể hiện được xử lý lỗi và bù trừ cho các thay đổi đã hoàn tất khi bước sau thất bại.
- Không tự thêm nghiệp vụ đơn hàng, thanh toán, trừ kho hoặc sửa tài khoản chỉ để có nhiều bước Saga. Nếu đề không đủ căn cứ cho tác động liên service, nêu phương án cụ thể và xác nhận phần nghiệp vụ đó với người dùng trước khi triển khai.
- Mỗi bước ghi dữ liệu dùng transaction cục bộ của service; không dùng transaction chung xuyên database hoặc two-phase commit.
- Thiết kế phải xét request/event trùng lặp, timeout và trường hợp bù trừ thất bại. Ghi rõ giới hạn nếu bản demo chưa xử lý đầy đủ.
- Không báo thao tác thành công khi trạng thái Saga chưa đáp ứng điều kiện hoàn tất đã định nghĩa.

## Quy ước triển khai

Các mục dưới đây là hướng dẫn làm việc, không phải công nghệ bổ sung bắt buộc từ đề.

- Kiểm tra cấu trúc và thay đổi hiện có trước khi sửa; không ghi đè công việc của người dùng.
- Khi dự án đã có cấu trúc hoặc công cụ build, tiếp tục theo quy ước đó. Tách API/controller, xử lý nghiệp vụ, truy cập dữ liệu và DTO ở mức phù hợp.
- Không trả trực tiếp entity tài khoản chứa thông tin nhạy cảm. Chuẩn hóa phản hồi lỗi và dùng HTTP status phù hợp.
- Đưa thông tin kết nối database, khóa ký và bí mật vào cấu hình môi trường; chỉ commit cấu hình mẫu không chứa bí mật thật.
- Không tự chọn thêm message broker, service discovery, Docker hoặc thư viện bảo mật như một yêu cầu của đề. Ghi lý do nếu cần đưa vào giải pháp.
- Ghi các phiên bản, port, biến môi trường, thứ tự khởi động và lệnh build/run thực tế vào README khi triển khai; không viết lệnh chưa được xác minh như thể đã chạy thành công.
- Giữ thay đổi trong phạm vi assignment; không xóa/reset database hoặc dữ liệu hiện có khi chưa được cho phép.

## Kiểm thử và tiêu chí hoàn tất

- Cung cấp Postman collection và cấu hình môi trường mẫu với base URL trỏ tới Gateway; dùng script assertion để kiểm tra kết quả, không chỉ lưu các request.
- Kiểm thử danh sách công khai, đầy đủ trường và `CategoryName`; bản ghi mới nằm đầu danh sách.
- Kiểm thử đăng nhập thành công/thất bại, tài khoản không hoạt động và phân quyền: chưa đăng nhập, Staff, Member không được thêm/sửa/xóa; Administrator thực hiện được.
- Kiểm thử thêm/sửa với dữ liệu hợp lệ và các biên: `Price = 100` được chấp nhận, giá dưới 100 bị từ chối; tên 10 ký tự bị từ chối, 11 ký tự được chấp nhận nếu các trường khác hợp lệ; ngày khác hôm nay và trường bị thiếu bị từ chối.
- Kiểm thử ID không tồn tại và tham chiếu danh mục/thương hiệu không hợp lệ.
- Kiểm thử Saga thành công và lỗi giữa chừng, xác minh dữ liệu sau bù trừ ở các service tham gia. Chỉ dùng dữ liệu thử nghiệm có thể nhận diện để dọn dẹp an toàn.
- Chạy build và các kiểm thử liên quan tới thay đổi. Báo rõ kiểm tra nào đã chạy, kết quả nào đạt và phần nào chưa xác minh do thiếu SQL Server hoặc dịch vụ phụ thuộc.
- Không coi việc chỉ biên dịch thành công hoặc gọi API CRUD thành công là bằng chứng đã hoàn thành yêu cầu Saga.
