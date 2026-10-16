# AURORA v2 – Giao diện nhiều trang, không còn popup

## Bắt đầu

1. Giải nén ZIP vào một thư mục mới (không chép đè lên thư mục bản cũ nếu chưa sao lưu).
2. Cần cài Java 21 (mở CMD và chạy `java -version`).
3. Nhấp đúp `START_DEMO_WINDOWS.bat` và giữ cửa sổ CMD mở.
4. Mở `http://127.0.0.1:8080` trong trình duyệt.

Nếu trước đó máy vẫn chạy AURORA phiên bản cũ trên cổng 8080, hãy tắt cửa sổ CMD cũ bằng **Ctrl+C** trước khi chạy v2. Nếu Chrome còn giữ giao diện cũ, dùng **Ctrl+Shift+R** để tải lại toàn bộ trang.

## Luồng trang mới

```text
Trang chủ (/)
  ├── Đăng nhập (/login)
  └── Đăng ký (/register)
        │
        ▼
  Sau khi đăng nhập
        │
        ▼
  Danh sách (/auctions)
        │
        ├── Chi tiết (/auctions/{id})
        │     ├── Đặt giá, auto-bid, lịch sử, biểu đồ
        │     └── Sửa sản phẩm (/auctions/{id}/edit) [Seller có quyền]
        │
        └── Đăng sản phẩm (/sell/new) [Seller/Admin]
```

- Khách chưa đăng nhập chỉ xem trang giới thiệu. Truy cập một URL đấu giá trong trình duyệt lúc chưa đăng nhập sẽ được chuyển về `/login`.
- Sau đăng nhập, bấm từng sản phẩm chuyển sang URL chi tiết độc lập. Dùng nút quay lại để về danh sách.
- Không có hộp thoại modal đăng nhập hay xem sản phẩm. Xác nhận xóa/hủy hiển thị ngay trên trang.
- Dữ liệu và quy tắc đặt giá, tranh chấp, auto-bid, anti-sniping, SSE vẫn xử lý ở server Java như trước.

## Tài khoản demo

- Seller: `seller_demo` / `demo12345` — tạo sản phẩm, quản lý phiên.
- Bidder: `bidder_demo` / `demo12345` — tham gia đấu giá.
- Có thể dùng trang `/register` để đăng ký thêm tài khoản.

## Đã kiểm tra / chưa kiểm tra

**Đã kiểm tra tại môi trường xây dựng:** biên dịch Java 21, chạy server HTTP JAR, 7 đường dẫn HTML, CSS/JS, kiểm thử nghiệp vụ lõi, phân quyền, bid đồng thời, auto-bid, SSE. Ảnh giao diện mới được render bởi Chromium từ HTML/CSS với dữ liệu minh họa.

**Cần xác nhận trên Windows:** thao tác đăng nhập/đăng ký và điều hướng thật bằng Chrome/Edge, quản lý tài khoản nhiều tab, client JavaFX. Môi trường xây dựng không cho Chromium truy cập localhost để làm kiểm thử end-to-end trình duyệt.

## Nguồn cần xem khi bảo vệ

- `server/src/main/java/vn/uet/auction/web/ApiServer.java`: ánh xạ URL → từng file HTML.
- `server/src/main/resources/web/index.html`: trang giới thiệu.
- `server/src/main/resources/web/login.html` và `register.html`: biểu mẫu tài khoản.
- `server/src/main/resources/web/auctions.html`: danh sách sản phẩm.
- `server/src/main/resources/web/detail.html`: trang chi tiết phiên.
- `server/src/main/resources/web/create.html`, `edit.html`: dành cho người bán.
- `server/src/main/resources/web/app.js`: xác thực trạng thái đăng nhập, điều hướng URL, xử lý form, REST và SSE.
- `server/src/main/resources/web/style.css`: toàn bộ giao diện.
