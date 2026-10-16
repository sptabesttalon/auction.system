# Phạm vi, hạn chế và roadmap kỹ thuật

## Các điểm đã kiểm thử trong môi trường tạo project

- JDK **21**: compile tất cả class Java server thành công bằng `javac`.
- Script `scripts/test-core.sh` biên dịch và chạy kiểm thử tích hợp lõi: đăng ký/đăng nhập, RBAC, đặt giá, từ chối rollback, cạnh tranh cùng giá, auto-bid, JSON parser → **PASSED**.
- HTTP server có thể khởi động và `GET /api/health`, `GET /api/auctions` trả JSON thực tế.

## Các điểm chưa kiểm chứng tại môi trường tạo project

- Maven, JUnit 5 test suite, GitHub Actions CI: môi trường không có Maven và DNS bên ngoài bị khóa; các file cấu hình đã được viết nhưng **chưa thể chạy**.
- JavaFX client: code FXML/controller đã viết nhưng chưa thể biên dịch/chạy JavaFX ở môi trường thiếu thư viện và desktop GUI tương thích; **cần xác nhận trên Windows máy bạn**.
- SQLite JDBC: có mã nguồn và Maven runtime dependency, chưa chạy JDBC thật do thiếu `sqlite-jdbc` jar; file serialization mode đã chạy được.
- Web UI trong Chromium: bản xem trước được render với **mock API** (4 thẻ sản phẩm, mở modal đăng nhập, không có JavaScript page error). Thử mở server localhost bằng Chromium của môi trường chạy bị chặn bởi chính sách môi trường (`ERR_BLOCKED_BY_ADMINISTRATOR`). Vì vậy **chưa xác minh thao tác browser kết nối server thật**; cần test trên máy của bạn.

## Chưa hỗ trợ và không được nhận là đã có

- Gateway thanh toán thật, vận chuyển và tranh chấp, KYC, tiền thật.
- HTTPS/TLS termination, audit logs, brute-force throttling, application production hardening.
- Auto-bid cho hàng ngàn bước giá nhỏ (giới hạn 500 vòng); có PriorityQueue nhưng chưa tối ưu vòng lặp giá cao.
- Cơ sở dữ liệu normalized (`users/items/auctions/bids` nhiều bảng), migration/versioning và khóa hàng theo DB.
- Multi-server deployment; `synchronized` không bảo đảm nhất quán giữa nhiều server processes.
- Email thông báo, phục hồi mật khẩu, avatar/upload ảnh; product dùng emoji minh họa.
- Tải lại snapshot khi kết nối SSE reconnect thành công nhưng không có event mới.
- Tự động xử lý tiền thanh toán: trạng thái PAID là xác nhận thủ công.
- Giới hạn dữ liệu bền vững: một snapshot serialized nguyên khối, copy-on-write khiến chi phí thao tác tăng theo kích thước dữ liệu.

## Roadmap nếu muốn nâng lên “đồ án tốt nghiệp” production-grade

1. Chuyển JDBC SQLite BLOB thành schema normalized, migration Flyway/Liquibase, ràng buộc và transaction per-auction.
2. Dùng PostgreSQL transaction with `SELECT FOR UPDATE`, optimistic versioning và outbox pattern để publish SSE/WebSocket.
3. Cải tiến auto-bid proxy để không cần hàng nghìn vòng lặp; có thể dựa vào hai mức max cao nhất, tối ưu so sánh top2 và tránh giới hạn 500 bước.
4. Đưa login về OIDC, HTTPS, server-side session store; chống brute force, rate limiting, audit/security logs.
5. Kiểm thử JUnit/Mockito/Testcontainers, concurrency stress (100–1000 client), chaos/restart tests; đo coverage bằng JaCoCo.
6. Docker Compose backend/PostgreSQL, CI/CD thực tế và monitoring metrics/traces.
7. Thiết kế web accessibility, responsive testing, sản phẩm có ảnh, email, thông báo cá nhân, upload an toàn.

Không cần làm hết các mục production cho **bài tập lớn môn học**; ưu tiên chứng minh đúng điểm bắt buộc và để mọi thành viên hiểu toàn bộ hệ thống.
