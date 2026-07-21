# 🎟️ Concert Ticket Booking Platform (Flash Sale Ready)

> Một hệ thống Backend được thiết kế chuyên biệt để chịu tải cao trong các chiến dịch **Flash Sale (Mở bán vé giới hạn)**. Thay vì tập trung vào những tính năng CRUD cơ bản, dự án này phô diễn các kỹ thuật kiến trúc phần mềm cấp cao (Enterprise-level) để giải quyết bài toán Concurrency, Bottleneck và Data Integrity.

## 🌟 6 "Vũ Khí" Công Nghệ Cốt Lõi (Architecture Highlights)

### 1. Xử lý Đồng thời & Khóa phân tán (Concurrency & Distributed Lock)
* **Vấn đề:** 50.000 user cùng tranh nhau 1 tấm vé cuối cùng.
* **Giải pháp:** Sử dụng **Redisson Distributed Lock** (khóa trên Redis) kết hợp với **Pessimistic Write Lock** (của PostgreSQL) để đảm bảo tuyệt đối không bao giờ bán lố vé (Overselling) hay dùng lố Voucher.

### 2. Xử lý Bất đồng bộ (Async Event-Driven với Kafka)
* **Vấn đề:** Database (PostgreSQL) sẽ sập ngay lập tức nếu phải ghi 50.000 dòng dữ liệu cùng lúc.
* **Giải pháp:** API Đặt vé chỉ mất **2ms** để đẩy Request vào **Kafka Topic** và trả về `202 Accepted` cho user. Một luồng Consumer chạy ngầm sẽ từ từ rút vé từ Kafka để ghi vào Database, giúp hệ thống không bao giờ bị quá tải (Peak Shaving).

### 3. Hàng đợi Cứu hộ (Dead Letter Queue - DLQ)
* **Vấn đề:** Nếu đang xử lý dở dang mà Database mất kết nối thì đơn hàng của user sẽ bị mất vĩnh viễn.
* **Giải pháp:** Bọc thép bằng **Kafka DLQ**. Các đơn lỗi được tự động ném sang Topic `booking-requests-dlq` và lưu vào Database khi hệ thống phục hồi. Admin có thể xem lại và ấn nút **Retry** để xử lý lại đơn hàng đó.

### 4. Máy Trạng thái & CronJob Hủy vé (State Machine & Scheduler)
* **Vấn đề:** Khách hàng đầu cơ, giữ vé (Reserved) nhưng không thanh toán, làm người khác không mua được.
* **Giải pháp:** Viết một CronJob (`@Scheduled`) chạy ngầm mỗi phút. Quét các đơn chưa thanh toán quá thời gian quy định -> Chuyển thành `FAILED` -> Trả lại số lượng vé vào DB và Redis (Inventory Rollback).

### 5. Tối ưu Đọc bằng Caching (Read-Heavy Optimization)
* **Vấn đề:** 99% lượng truy cập là F5 liên tục để XEM danh sách sự kiện, chỉ 1% là thực sự MUA.
* **Giải pháp:** Cấu hình **Spring Cache + Redis**. Nén danh sách sự kiện thành JSON lưu trên RAM (có TTL chống data cũ). Kết quả: API Đọc trả về trong 1-2ms, Database hoàn toàn "ngủ yên".

### 6. Chống DDoS/Spam bằng Rate Limiting (Token Bucket)
* **Vấn đề:** Hacker dùng Tool/Bot bắn 1000 request/giây để gom vé.
* **Giải pháp:** Viết Interceptor sử dụng **Redisson RateLimiter** chặn ở tầng ngoài cùng. Giới hạn cứng mỗi IP chỉ được gọi API Đặt vé tối đa 5 lần/giây. Vượt quá sẽ bị chặn với mã `429 Too Many Requests`.

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

* **Core:** Java 17, Spring Boot 3.2.x, Spring Data JPA
* **Database:** PostgreSQL 16 (Port `5433` - Tránh xung đột máy Local)
* **Cache & Lock & Rate Limit:** Redis 7 + Redisson
* **Message Broker:** Apache Kafka 3.7 (Kraft mode)
* **Observability:** Prometheus + Grafana (Auto-provisioned Dashboard)
* **Testing:** JUnit 5, H2 In-memory DB, ExecutorService (Multi-threading Test)

---

## 🚀 Hướng dẫn Cài đặt & Chạy Hệ thống

### 1. Khởi động Hạ tầng (Infrastructure)
Bạn chỉ cần đúng 1 lệnh để dựng toàn bộ DB, Cache, Message Broker và Monitoring:
```bash
docker-compose up -d
```

### 2. Chạy Ứng dụng Spring Boot
```bash
./mvnw clean spring-boot:run
```
*(Hệ thống đã tích hợp sẵn Database Seeder, tự động tạo sẵn Concert, Ticket, Voucher ngay khi chạy).*

---

## 📸 Hướng dẫn Test bằng Postman (Kịch bản chấm bài)

Toàn bộ API đã được cấu hình sẵn. Hãy import file `api_testing/postman_collection.json` vào Postman.

### Kịch bản 1: Test chống Spam/Bot (Rate Limiting)
1. Trong Postman, mở thư mục **Booking API**.
2. Chuột phải vào thư mục -> Chọn **Run folder** (Tính năng Runner).
3. Đặt Iterations là `20`, Delay `0ms`. Bấm **Run**.
4. **Kết quả:** Vài request đầu tiên trả về xanh (`202 Accepted`). Các request sau lập tức bị chặn đỏ rực (`429 Too Many Requests`).

### Kịch bản 2: Test CronJob "Nhả Vé" (Auto Release)
1. Gọi API **Submit Booking Request** (Ví dụ: `concertId: 1`, `categoryId: 1`). Copy mã `requestId` trả về.
2. **Đừng thanh toán!** Hãy chờ đúng **1 phút**.
3. Nhìn vào Console của Spring Boot, bạn sẽ thấy log: `Releasing tickets for expired order...`
4. Dùng API **Get Booking Status** với mã `requestId` đó. **Kết quả:** Status đã bị đổi thành `FAILED` và vé đã được nhả lại kho.

### Kịch bản 3: Test Thanh toán thành công (State Machine)
1. Gọi lại API **Submit Booking Request**, copy mã `requestId`.
2. Lập tức gọi API **Pay Order** (Dán `requestId` vào URL).
3. **Kết quả:** Đơn hàng chuyển sang `COMPLETED`, vé chuyển sang `SOLD`. Cronjob sẽ không bao giờ đụng đến đơn này nữa.

### Kịch bản 4: Test Dead Letter Queue (DLQ)
1. Chỉnh sửa Body của API **Submit Booking Request**: Đổi `concertId` thành `999` (Số ảo).
2. Gọi API. Vẫn nhận `202 Accepted` (Vì Kafka đã nhận đơn).
3. Mở thư mục **Admin API** -> Gọi **Get DLQ Messages**.
4. **Kết quả:** Đơn hàng bị kẹt đã được ném sang DLQ và lưu vào Database với lý do lỗi: `Concert not found` để Admin xử lý sau.

### Kịch bản 5: Test Redis Cache (Bảo vệ Database)
1. Gọi API **Get All Concerts** lần 1. (Nhìn Console thấy câu lệnh SQL `select ...`).
2. Gọi API lần 2, 3, 4 liên tục.
3. **Kết quả:** Tốc độ trả về dưới 5ms, và Console **tuyệt đối không in ra thêm câu SQL nào**. Dữ liệu được lấy 100% từ RAM.

---

## 📈 Giám sát Hệ thống (Monitoring)
Mở trình duyệt: `http://localhost:3000`
* **Username/Password:** `admin` / `admin`
* Vào **Dashboards** -> **Spring Boot Dashboards** -> **JVM (Micrometer)**.
* Khi bạn dùng Postman bắn liên tục vào API, biểu đồ CPU và Request sẽ **nhảy vọt (Spike)** cực kỳ trực quan và chuyên nghiệp.

> **Note:** Hệ thống chủ động **Lược bỏ (Scope out)** tính năng Login/JWT Authentication. Trong kiến trúc Microservices thực tế, Authentication được xử lý ở tầng API Gateway. Việc bỏ JWT giúp ban giám khảo test tính năng Flash Sale (phần quan trọng nhất) một cách mượt mà nhất mà không phải xin cấp lại Token.
