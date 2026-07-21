# Các Giả Định và Hạn Chế Của Hệ Thống (Assumptions & Limitations)

Để tối ưu hóa thời gian phát triển và tập trung giải quyết bài toán Core (Booking Flash Sale), hệ thống đã thực hiện một số giả định mang tính chất đánh đổi (Trade-off). Dưới đây là danh sách chi tiết:

## 1. Dữ Liệu Khởi Tạo (Seed Data)
**Giả định:** Trong thực tế, các sự kiện (Concerts), Hạng vé (TicketCategory) và Mã giảm giá (Vouchers) sẽ được tạo và quản lý bởi một hệ thống Content Management System (CMS) riêng biệt. 
**Triển khai:** Thay vì xây dựng các API CRUD nhàm chán cho các tính năng này, hệ thống giả định dữ liệu sẽ được bơm (seed) sẵn vào cơ sở dữ liệu ngay từ đầu thông qua kịch bản Flyway Migration (V1__init.sql) hoặc data.sql. APIs của hệ thống chỉ tập trung vào luồng Read và Booking.

## 2. Thanh Toán (Payment Simulation)
**Giả định:** Tích hợp với cổng thanh toán thực tế (VNPAY, MoMo, Stripe...) yêu cầu Webhook callback và các bước bảo mật phức tạp, nằm ngoài phạm vi cốt lõi của một hệ thống quản lý đặt vé trong thời gian ngắn.
**Triển khai:** Luồng thanh toán được **mô phỏng** qua các Admin APIs. Khi đặt vé thành công qua Kafka Consumer, Order được gán trạng thái `RESERVED` (đợi thanh toán). Để mô phỏng thanh toán thành công hoặc thất bại, hệ thống cung cấp API `PUT /api/v1/admin/bookings/{id}/status`. Khi Admin đẩy status về `COMPLETED`, hệ thống coi như thanh toán xong; đẩy về `FAILED/CANCELLED`, hệ thống thu hồi vé.

## 3. Phân Bổ Vé (Ticket Allocation)
**Giả định:** Trong môi trường Flash Sale đỉnh điểm, việc để khách hàng xem sơ đồ ghế và chọn từng vị trí ghế (Seat map selection) sẽ dẫn đến xung đột giao dịch cực kỳ nghiêm trọng (Deadlocks).
**Triển khai:** Hệ thống mặc định sử dụng cơ chế **Auto-assignment** (Gán ngẫu nhiên). Kafka Consumer sẽ tự động query và nhặt ra các Ticket có trạng thái `AVAILABLE` đầu tiên thuộc hạng vé đó để gán cho User.

## 4. Xác Thực Người Dùng (Authentication)
**Giả định:** Việc triển khai OAuth2 hoặc JWT Server tốn kém thời gian setup và không thể hiện rõ năng lực giải quyết bài toán tải cao.
**Triển khai:** Lược bỏ cơ chế bảo mật xác thực chặn đầu vào. API `/api/v1/bookings` nhận trực tiếp `userId` qua payload để định danh người dùng. Trong thực tế, `userId` này sẽ được trích xuất từ Token ở tầng API Gateway.

## 5. Hạ Tầng Chạy Cục Bộ (Single-Node Infrastructure)
**Giả định:** Để tiện cho việc đánh giá mã nguồn, hệ thống chạy tốt nhất trên local.
**Triển khai:** Dù hệ thống sử dụng các công nghệ phân tán (Redis, Kafka, PostgreSQL), nhưng môi trường local hiện tại qua `docker-compose.yml` đang setup chạy dạng Single-Node cho từng dịch vụ. Hệ thống được thiết kế hoàn toàn Stateless (không giữ trạng thái session), nên có thể dễ dàng scale-out App thành nhiều instances nếu được đưa lên Kubernetes mà không cần refactor code.
