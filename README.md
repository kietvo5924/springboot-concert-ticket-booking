# 🎟️ Concert Ticket Booking Platform (Flash Sale Backend)

Dự án Backend chuyên biệt xử lý hệ thống đặt vé với lưu lượng truy cập cao (Flash Sale), giải quyết bài toán Concurrency, Bottleneck và đảm bảo Data Integrity.

## 🛠️ Hạ tầng & Thông tin Kết nối (Infrastructure & Ports)

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

## 🚀 Hướng dẫn Cài đặt & Chạy Hệ thống

**Bước 1: Khởi động toàn bộ Hạ tầng (Database, Message Broker, Monitoring)**
```bash
docker-compose up -d
```

**Bước 2: Khởi động Ứng dụng Spring Boot**
*(Database Seeder sẽ tự động tạo sẵn dữ liệu mẫu về Concert, Ticket, Voucher).*
```bash
./mvnw clean spring-boot:run
```

---

## 📸 Hướng dẫn Test Nghiệp Vụ (Bằng Postman)

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

## 🌟 Tóm tắt Kiến trúc (Architecture Highlights)

* **Idempotency:** Ngăn chặn Double-payment bằng Idempotency Key lưu trên Redis.
* **Concurrency:** Kết hợp **Redisson Distributed Lock** và **Pessimistic Lock** ngăn chặn Overselling khi 50.000 user cùng tranh 1 vé.
* **Asynchronous:** Chuyển đổi mô hình Request-Response truyền thống sang Event-Driven với **Kafka**, giúp API phản hồi trong 2ms để chống Peak-Traffic.

> **Note:** Tính năng xác thực (JWT/Login) chủ động được tinh giản, giả định hệ thống sử dụng kiến trúc Microservices và Authentication được xử lý tập trung tại tầng API Gateway. Việc này giúp ban giám khảo tập trung chấm điểm năng lực xử lý nghiệp vụ Flash Sale.
