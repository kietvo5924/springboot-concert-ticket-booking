# Thiết Kế Kiến Trúc & Cơ Sở Dữ Liệu Hệ Thống

Tài liệu này giải thích chi tiết các quyết định về kiến trúc và thiết kế hướng luồng dữ liệu nhằm giải quyết bài toán **Flash Sale** (50.000 users, 300-500 requests/minute) một cách hiệu quả và an toàn.

## 1. Mô Hình Kiến Trúc Lõi (3-Layer Architecture)
Hệ thống tuân thủ chặt chẽ kiến trúc 3 tầng truyền thống của Spring Boot:
- **Controller Layer**: Chỉ làm nhiệm vụ tiếp nhận HTTP Request, Validate Input và trả về HTTP Response. Không chứa logic nghiệp vụ.
- **Service Layer**: Đảm nhiệm toàn bộ logic nghiệp vụ (Idempotency, Khóa phân tán, Business Rule, Kafka Produce/Consume).
- **Repository Layer**: Trực tiếp giao tiếp với PostgreSQL thông qua Spring Data JPA.

*Trade-off*: Kiến trúc 3-Layer mang lại tốc độ phát triển (Time-to-market) nhanh gọn, phù hợp cho bài toán có thời gian giới hạn thay vì các kiến trúc phức tạp mang nặng overhead như Hexagonal/Clean Architecture, nhưng vẫn đảm bảo tính phân tách quan tâm (Separation of Concerns).

## 2. Chiến Lược Chống Overselling bằng Redis Distributed Lock
Trong môi trường Flash Sale, hàng ngàn Request sẽ đồng thời tranh giành một số lượng vé có hạn. Nếu sử dụng Database Lock (`Pessimistic Lock - SELECT FOR UPDATE`), Database Connection Pool sẽ nhanh chóng bị quá tải, gây nghẽn cổ chai (Bottleneck) và sập toàn hệ thống.

**Giải pháp:**
1. Khóa phân tán (Distributed Lock) bằng `Redisson` được áp dụng ở mức độ ứng dụng, khóa theo `ticketCategoryId` để đảm bảo không có hai luồng Thread nào tranh chấp xử lý tồn kho cùng một lúc cho một hạng vé.
2. Tồn kho (Inventory) được quản lý qua `RAtomicLong` của Redis. Thao tác `decrementAndGet()` diễn ra trên RAM (In-memory) cực kỳ nhanh, giúp loại bỏ hoàn toàn độ trễ I/O của Database.

## 3. Luồng Xử Lý Bất Đồng Bộ (Asynchronous) bằng Apache Kafka
Để đáp ứng SLA phản hồi cực nhanh cho Client, HTTP Thread không thể chờ đợi việc ghi dữ liệu vào CSDL (vốn rất chậm và tốn tài nguyên).

**Cơ chế hoạt động (Shock Absorber):**
1. HTTP Controller tiếp nhận Request, kiểm tra Idempotency, trừ tồn kho trên Redis và lập tức sinh ra một **Kafka Message** (chứa thông tin đơn hàng).
2. Controller ngay lập tức trả về `202 Accepted` cho Client (với `requestId`). API Response Time thường chỉ dưới 50ms.
3. Ở Background, **Kafka Consumer** sẽ kéo (pull) các tin nhắn này theo thứ tự. Tầng Consumer sẽ chậm rãi thực hiện các thao tác nặng nề như: Map Ticket vào Order, cập nhật status, ghi xuống PostgreSQL. Nếu lưu lỗi, Consumer thực hiện thao tác **Rollback** trả lại vé cho Redis.

*Trade-off*: Eventual Consistency (Nhất quán cuối). Dữ liệu vé và order ở Database sẽ bị "trễ" một khoảnh khắc nhỏ so với thực tế ở Redis. Tuy nhiên, đổi lại hệ thống có khả năng chịu tải siêu việt và chống đứt gãy luồng HTTP (Thread Exhaustion).

## 4. Thiết Kế Cơ Sở Dữ Liệu (PostgreSQL)
Lược đồ (Schema) CSDL được chuẩn hóa (Normalized) bao gồm các thực thể chính:
- `Concert`: Thông tin sự kiện.
- `TicketCategory`: Hạng vé, giá vé, số lượng ban đầu và số lượng còn lại.
- `Ticket`: Đại diện cho từng vé vật lý (Có mã ghế ngồi, trạng thái).
- `Order`: Lưu thông tin thanh toán, voucher và thông tin user.

Một **Order** có thể chứa nhiều **Ticket**, và áp dụng một **Voucher**. Logic giải phóng vé (Rollback) khi đơn hàng hủy được thiết kế liên đới chặt chẽ: Khi Order `FAILED/CANCELLED`, các Tickets bên trong lập tức bị cắt đứt quan hệ (set `order_id = null`) và chuyển về lại `AVAILABLE`.
