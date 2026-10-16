# Nhật ký kiểm thử lại — 16/10/2026

## Đã chạy thành công (JDK 21, Linux)

```text
$ bash scripts/test-core.sh
PASSED: registration, authentication, permissions, bidding, no rollback, concurrent bid, auto-bid, JSON parsing

$ python3 scripts/test-http.py
PASS: HTTP register/login, seller creation, RBAC, invalid bid, concurrent equal bids, auto-bid, SSE push

$ python3 scripts/preview-web.py
Cards rendered: 4 Errors: []
Login visible: 1 Errors: []
```

- `test-http.py` khởi chạy server HTTP thực trên cổng ngẫu nhiên và sử dụng database tạm, kiểm tra kết nối SSE, 2 request cạnh tranh cùng giá (1 success, 1 reject), auto-bid, phân quyền và HTML.
- `preview-web.py` kiểm tra việc render trong Chromium với **API mô phỏng**. Không dùng kết quả này để khẳng định browser đã thực hiện E2E thật với server.
- Kiểm tra báo cáo PDF: 14 trang A4, mở được qua PyMuPDF và không có mã hóa.

## Chưa chạy/không khẳng định đã pass

- JavaFX UI: chưa có JavaFX libraries, máy chủ GUI desktop và Maven ở môi trường này.
- Maven/JUnit/CI: chưa chạy `mvn verify`, nên GitHub Actions chưa được chứng minh là xanh.
- SQLite JDBC: chưa test thật với driver Maven.
- End-to-end trong Chromium đến server localhost: trình duyệt trả `net::ERR_BLOCKED_BY_ADMINISTRATOR`, dù Python HTTP client kết nối cùng server thành công.

## Kiểm thử nhóm cần thực hiện trên Windows trước bảo vệ

1. Mở website từ script JDK-only; thử đăng nhập Seller và tạo phiên, một Bidder đấu giá.
2. Mở cùng website ở 2 browser contexts khác nhau; bid cạnh tranh và kiểm tra giá, biểu đồ, SSE cập nhật.
3. Cài Maven, chạy `mvn verify`; chạy `mvn -pl client-javafx javafx:run` và kiểm tra FXML, biểu đồ.
4. Chạy thử Anti-sniping và kết thúc trạng thái thời gian thực; khởi động lại server và xác nhận dữ liệu còn nguyên.
5. Push GitHub repo với commit tiến độ thật, kiểm tra GitHub Actions workflow.

## Gói chạy độc lập

- Tạo `dist/AURORA-server.jar` với JDK 21 (`jar --create --main-class ...`).
- Khởi động lại từ JAR cùng dữ liệu demo trong thư mục tạm; kiểm tra `GET /api/auctions` trả **4 phiên** và `GET /` phục vụ trang HTML: **PASSED**.
- `scripts/run-linux-jdk-only.sh`: biên dịch lại nguồn bằng JDK 21, khởi động server và trả JSON health check: **PASSED**.
- Windows `.bat` chưa thực thi trực tiếp trên Windows trong môi trường này; cần xác nhận lần cuối tại máy bạn.

## Bổ sung kiểm thử giao diện nhiều trang v2 (16/10/2026)

```text
$ bash scripts/build-jar-jdk21.sh
Created dist/AURORA-server.jar
$ python scripts/test-multipage.py
PASS: seven real HTML routes, CSS/JS, protected hidden pages, no modal markup, unknown path 404
$ bash scripts/test-core.sh
PASSED: registration, authentication, permissions, bidding, no rollback, concurrent bid, auto-bid, JSON parsing
$ python scripts/test-http.py
PASS: HTTP register/login, seller creation, RBAC, invalid bid, concurrent equal bids, auto-bid, SSE push
```

- Tạo lại ảnh giao diện home/login/catalog/detail bằng Chromium và HTML/CSS; **catalog/detail dùng dữ liệu mô phỏng**, không thay thế end-to-end thực tế.
- Chưa thực hiện được thao tác giao diện Chromium kết nối localhost server thực, vì chính sách mạng của trình duyệt trong môi trường kiểm thử (`ERR_BLOCKED_BY_ADMINISTRATOR`).
- Trên Windows, cần kiểm chứng chuyển trang khi đăng nhập, refresh trang `/auctions/ID`, lịch sử, auto-bid và tạo/xóa sản phẩm.
