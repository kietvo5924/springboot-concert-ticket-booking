# 🎯 CHỈ THỊ HỆ THỐNG DÀNH CHO AI (BMAD / CODING ASSISTANT)
**Dự án:** Concert Ticket Booking Platform (Flash Sale Backend)
**Repository:** https://github.com/kietvo5924/springboot-concert-ticket-booking.git
**Ngữ cảnh:** Đây là một bài kiểm tra kỹ năng Backend Engineering, tập trung vào tư duy thiết kế hệ thống, khả năng xử lý tải cao, và cấu trúc mã nguồn sạch.

## ⚠️ CÁC QUY TẮC BẮT BUỘC (MUST-OBEY RULES)
1. **Bảo mật danh tính:** Tuyệt đối không để lại bất kỳ comment, tên biến, hoặc tài liệu nào chứa các từ khóa như "bmad", "AI generated", "ChatGPT", "prompt". Sau khi hoàn thành toàn bộ dự án, bạn (AI) phải tự động quét toàn bộ codebase và xóa sạch file chỉ thị này cũng như mọi dấu vết liên quan đến AI.
2. **Giải thích cặn kẽ:** Sau khi hoàn thành MỖI TASK trong danh sách bên dưới, bạn phải dừng lại và in ra một đoạn giải thích chi tiết cho người dùng bằng tiếng Việt. Cần nói rõ: Bạn đã code những gì? Tại sao lại chọn giải pháp đó (trade-off)? Đoạn code nào xử lý chống overselling, chống duplicate?
3. **Kiểm thử đầy đủ:** Mọi service cốt lõi (Core Business Logic) đều phải đi kèm Unit Test (sử dụng JUnit 5, Mockito) và đạt độ phủ code cao. Chú trọng test các case ngoại lệ (hết vé, sai voucher, request trùng lặp).

## 🛠 DANH SÁCH CÔNG VIỆC CẦN LÀM (TO-DO LIST)

### Task 1: Thiết lập nền tảng & Môi trường (Local Setup)
- Khởi tạo project Java Spring Boot với Clean Architecture hoặc MVC Layered.
- Viết file `docker-compose.yml` bao gồm: App, PostgreSQL/MySQL, Redis (cho Caching & Lock), và Apache Kafka (cho Event-driven). Đảm bảo hệ thống có thể chạy cục bộ dễ dàng.

### Task 2: Thiết kế Cơ sở dữ liệu (Database Design)
- Tạo các Entity JPA cơ bản: `Concert`, `TicketCategory`, `Ticket`, `Voucher`, `Order`.
- Định nghĩa rõ các trạng thái của `Order` (VD: INITIAL, RESERVED, COMPLETED, FAILED).

### Task 3: Phát triển Customer-facing APIs (Luồng người dùng)
- API: Xem danh sách sự kiện âm nhạc và xem hạng vé/giá vé.
- **API Đặt vé (Core) - Đòi hỏi độ phức tạp cao:**
  - Áp dụng Redis Distributed Lock để giải quyết rủi ro bán vượt quá số lượng vé (Overselling).
  - Implement cơ chế Idempotency (nhận một `requestId` duy nhất từ client) để chặn các request đặt vé trùng lặp do người dùng retry.
  - Đẩy request đặt vé vào Apache Kafka topic thay vì xử lý đồng bộ, giúp hệ thống chịu tải 300-500 requests/phút mà không sập.
- API: Áp dụng mã voucher khuyến mãi (Có check giới hạn số lượng và logic chống lạm dụng voucher).
- API: Theo dõi trạng thái đơn hàng.

### Task 4: Phát triển Internal Operation APIs (Luồng nội bộ)
*(Lưu ý: Chỉ làm các API cốt lõi, phần còn lại sử dụng Seed Data)*
- API: Theo dõi danh sách đơn đặt vé (Monitor bookings).
- API: Kiểm tra số lượng vé còn trống (Validate ticket availability).
- API: Cập nhật trạng thái đơn đặt vé bằng tay (Manual update).
- Xử lý Seed Data (Flyway/Data.sql): Tạo sẵn Concert, Ticket Categories, và Vouchers thay vì viết API quản lý CRUD toàn bộ.

### Task 5: Hoàn thiện Tài liệu & Bàn giao (Documentation)
- Tích hợp Swagger / OpenAPI để sinh tài liệu API tự động.
- Viết file `README.md` bao gồm:
  - Coding guideline (Cách viết 1 API mới, chuẩn naming convention).
  - Lệnh chạy Unit Test và cách xem report (ví dụ sử dụng JaCoCo).
  - Hướng dẫn chạy dự án ở local qua Docker.
- Xuất toàn bộ API ra file `postman_collection.json` và lưu vào thư mục nộp bài.