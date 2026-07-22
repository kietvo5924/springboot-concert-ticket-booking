# Concert Ticket Booking Platform (Flash Sale Backend)

Dự án Backend chuyên biệt xử lý hệ thống đặt vé với lưu lượng truy cập cao (Flash Sale), giải quyết bài toán Concurrency, Bottleneck và đảm bảo Data Integrity.

## Hạ tầng & Thông tin Kết nối (Infrastructure & Ports)

| Service | Môi trường | Cổng (Port) | Thông tin đăng nhập (User / Password) | Chức năng |
| :--- | :--- | :--- | :--- | :--- |
| **Spring Boot App** | Localhost | `8080` | Không có | Ứng dụng Backend chính |
| **PostgreSQL** | Docker | `5433` | `user` / `password` | Database chính lưu thông tin |
| **Redis** | Docker | `6379` | Không có | Caching, Distributed Lock, Rate Limiting |
| **Apache Kafka** | Docker | `9092` | Không có | Message Broker (Xử lý bất đồng bộ) |
| **Prometheus** | Docker | `9090` | Không có | Thu thập metrics hệ thống |
| **Grafana** | Docker | `3000` | `admin` / `admin` | Monitoring Dashboard |
| **Swagger UI** | Localhost | `8080` | `http://localhost:8080/swagger-ui/index.html` | Tài liệu API |

*(Ghi chú: Port PostgreSQL được đổi thành `5433` để tránh xung đột với DB Local có sẵn trên máy).*

---

## Hướng dẫn Cài đặt & Chạy Hệ thống

Tài liệu này hướng dẫn chi tiết các bước thiết lập môi trường và khởi chạy hệ thống trên máy tính cá nhân để phục vụ cho quá trình chấm điểm. 

### Bước 1: Yêu cầu Hệ thống Ban đầu
Để hệ thống vận hành trơn tru, máy tính cá nhân của người chấm bài cần được cài đặt sẵn:
- **Docker** và **Docker Compose** (Bắt buộc).
- **Java 21** (Chỉ cần thiết nếu bạn muốn chạy code và debug thủ công bằng phần mềm).

### Bước 2: Lựa chọn Khởi chạy Hệ thống
Bản thân mã nguồn Backend cũng đã được đóng gói sẵn vào Docker (được định nghĩa trực tiếp trong tệp `docker-compose.yml`). Kỹ sư chấm bài có thể khởi chạy toàn bộ dự án theo 1 trong 2 cách sau:

#### Cách 1: Chạy hệ thống tự động qua Docker (Khuyên dùng - 1 Click)
Đây là cách nhanh nhất. Docker sẽ tự động xây dựng (build) mã nguồn Backend, đồng thời khởi chạy luôn cả Database, Redis, Kafka, Prometheus và Grafana.
Mở màn hình dòng lệnh tại thư mục gốc và gõ:
```bash
docker-compose up -d --build
```
*(Lưu ý: Vui lòng chờ khoảng 30 giây để toàn bộ cụm Server khởi động hoàn tất và Backend tự động nạp dữ liệu vé mẫu).*

#### Cách 2: Chạy riêng Backend ở máy thật (Dành cho việc xem Code / Debug)
Nếu bạn muốn chạy mã nguồn thông qua phần mềm (IDE) để dễ dàng kiểm tra, bạn cần khởi động phần hạ tầng trước, nhưng phải **bỏ qua** container `backend` để tránh bị báo lỗi trùng cổng 8080:
```bash
docker-compose up -d postgres redis kafka prometheus grafana
```
Sau khi hạ tầng báo Done, hãy mở thư mục dự án bằng **IntelliJ IDEA** và bấm nút Run, hoặc gõ câu lệnh sau để chạy Backend thủ công:
```bash
./mvnw clean spring-boot:run
```

### Bước 3: Kiểm tra Cổng Giao tiếp và Tài liệu
Khi máy chủ Backend đã khởi động thành công, bạn có thể truy cập ngay vào các trang giao diện trực quan trên trình duyệt:
- **Swagger UI (Tài liệu API):** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Grafana (Giám sát hệ thống):** [http://localhost:3000](http://localhost:3000) (Tài khoản: `admin` / Mật khẩu: `admin`)

---

## Hướng dẫn Test Nghiệp Vụ (Bằng Postman)

Vui lòng Import file `api_testing/postman_collection.json` vào Postman để tiến hành test.

| Tính năng | Hướng dẫn Test | Kịch bản / Kết quả mong đợi |
| :--- | :--- | :--- |
| **1. Chống Spam / Rate Limiting** | Dùng **Postman Runner** chạy API *Submit Booking Request* với Iterations = `20`, Delay = `0ms`. | 5 request đầu báo `202 Accepted`. Các request sau bị chặn với lỗi `429 Too Many Requests`. |
| **2. DLQ (Dead Letter Queue)** | Sửa `concertId` thành `999` (số ảo) trong API *Submit Booking*. Sau đó gọi API *Get DLQ Messages*. | Đơn hàng không kẹt làm sập hệ thống mà bị ném vào DLQ với lỗi `Concert not found`. |
| **3. Redis Caching** | Gọi API *Get All Concerts* liên tục 5 lần. Quan sát Console Spring Boot. | Chỉ có lần đầu báo log SQL `select...`. Các lần sau tốc độ < 5ms và Database "ngủ yên". |
| **4. CronJob Tự động nhả vé** | Gọi API *Submit Booking* để giữ vé. **Chờ 1 phút** và gọi *Get Booking Status*. | Sau 1 phút, Console báo `Releasing tickets...`. Status của đơn hàng chuyển thành `FAILED`. |
| **5. State Machine (Thanh toán)**| Gọi API *Submit Booking*. Sau đó lập tức truyền `requestId` vào gọi API *Pay Order*. | Status chuyển sang `COMPLETED`, CronJob sẽ không bao giờ hủy đơn hàng này nữa. |
| **6. Giám sát hệ thống** | Truy cập `http://localhost:3000` (admin/admin) -> Mở Dashboard `JVM (Micrometer)`. Spam API. | Biểu đồ CPU, Heap Memory, Request nhảy vọt tương ứng với tải hệ thống thực tế. |

---

## Kiến trúc Hệ thống (Architecture & Solutions)

Để giải quyết bài toán Flash Sale với 50.000 users và 500 req/min, hệ thống áp dụng các pattern sau:
1. **Chống Overselling (Bán lố vé):** Kết hợp **Redisson Distributed Lock** (khóa ở tầng Cache) và **Pessimistic Lock** (khóa ở tầng Database).
2. **Chống Duplicate Bookings:** Sử dụng cơ chế **Idempotency Key** lưu trên Redis trong 5 phút. Nếu user bấm "Đặt vé" 2 lần liên tiếp, hệ thống sẽ chặn ngay lập tức.
3. **Chống Voucher Abuse:** Sử dụng `Pessimistic Lock` khi kiểm tra và trừ số lượng Voucher, đảm bảo không có tình trạng 2 người cùng dùng 1 mã voucher cuối cùng.
4. **Chống System Instability (Quá tải):** 
   - **Asynchronous Processing:** Sử dụng **Apache Kafka** làm Message Broker. Request đặt vé được đẩy vào Kafka và trả về `202 Accepted` ngay lập tức (phản hồi trong 2ms), giúp API không bị nghẽn.
   - **Rate Limiting:** Sử dụng thuật toán Token Bucket (Redis), giới hạn mỗi IP chỉ được gửi 5 requests/giây.
   - **Redis Caching:** Cache toàn bộ danh sách Concert ở Redis, giảm tải hoàn toàn cho Database ở các tác vụ Read-heavy.

*(Ghi chú: Để xem chi tiết lý luận thiết kế, sơ đồ luồng dữ liệu và sơ đồ cơ sở dữ liệu, vui lòng tham khảo File Word số 1 trong thư mục nộp bài).*

---

## Phạm vi & Giả định (Assumptions & Limitations)

Theo tinh thần tập trung vào Engineering Thinking và Core Value của bài toán Flash Sale, hệ thống được giới hạn phạm vi như sau:

**Những gì đã làm (In-scope):**
- **State Machine Đơn hàng:** Đơn hàng có 4 trạng thái (`RECEIVED` -> `RESERVED` -> `COMPLETED` hoặc `FAILED`). Hỗ trợ CronJob tự động hủy đơn `RESERVED` sau 1 phút nếu không thanh toán và hoàn trả vé.
- **Customer API:** Đặt vé (Flash sale an toàn), Xem danh sách Concert, Thanh toán, Tra cứu trạng thái.
- **Admin/Operation API:** Theo dõi lượng vé bán ra/tồn kho thực tế, xem danh sách đơn hàng, Quản lý Dead Letter Queue (DLQ) để xử lý các đơn lỗi (ví dụ: Concert không tồn tại).

**Những gì không làm (Out-of-scope / Assumptions):**
- **CRUD Vouchers & Concerts:** Hệ thống *không* cung cấp API để Operation Team Tạo/Sửa/Xóa Voucher hoặc Concert trên Dashboard. Thay vào đó, dữ liệu được tự động sinh ra (Seeding) lúc khởi động. Khách hàng vẫn có thể áp dụng Voucher hợp lệ khi đặt vé bình thường.
- **Authentication/Authorization (JWT):** Tính năng phân quyền (Login/Signup) được lược bỏ. Giả định hệ thống chạy trong kiến trúc Microservices và Authentication đã được xử lý tập trung tại tầng API Gateway.
- **Payment Gateway Integration:** API `/pay` hiện tại là mô phỏng (Mock). Khi gọi API này, hệ thống coi như thanh toán thành công và chốt vé.

*(Ghi chú: Để xem chi tiết bản giải trình về phạm vi công việc và các giả định hệ thống, vui lòng tham khảo File Word số 2 trong thư mục nộp bài).*
