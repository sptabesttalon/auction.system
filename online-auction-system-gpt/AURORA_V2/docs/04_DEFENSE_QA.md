# Kịch bản bảo vệ và 25 câu hỏi khó

## Mở đầu 90 giây — có thể tự diễn đạt bằng lời

> Đồ án của nhóm em là AURORA, hệ thống đấu giá trực tuyến xây dựng bằng Java 21 theo mô hình Client–Server và MVC. Nhóm em có hai loại client: website và JavaFX sử dụng FXML theo yêu cầu môn học. Tất cả các thao tác quan trọng — xác thực người dùng, kiểm tra giá, xác định người thắng, đóng phiên — đều xử lý ở server. Điểm kỹ thuật trọng tâm là đảm bảo tính đúng đắn khi nhiều người đặt giá cùng lúc: service sử dụng synchronized kết hợp copy–validate–persist–publish, nên không có hai giao dịch cùng ghi đè giá. Khi giao dịch đã lưu thành công, Observer truyền sự kiện SSE đến các client đang theo dõi. Nhóm em cũng thực hiện đặt giá tự động, gia hạn phút cuối và vẽ biểu đồ lịch sử giá. Em sẽ trình bày kiến trúc, quy tắc đặt giá, sau đó mở hai client để demo concurrency và realtime.

**Không học thuộc từng chữ**; hiểu quy trình rồi tự diễn đạt. Đặc biệt, không nói “đã đạt 100% điểm” hoặc “đã deploy production” khi chưa có bằng chứng.

## Câu hỏi - trả lời

1. **Vì sao chọn Java thay vì Node.js?** Đề yêu cầu Java/OOP/JavaFX, vì vậy server Java 21 đáp ứng trực tiếp; built-in HttpServer đủ minh họa REST và SSE.
2. **MVC là gì?** Model biểu diễn dữ liệu/nghiệp vụ, View hiển thị, Controller xử lý yêu cầu người dùng. Ở đây có MVC phía JavaFX và controller/service/DAO phía server.
3. **Client có truy cập DB trực tiếp không?** Không. Client chỉ gửi HTTP JSON; DAO tồn tại hoàn toàn trên server.
4. **Vì sao còn làm web khi đề yêu cầu JavaFX?** Web là client mở rộng trên cùng API, còn JavaFX/FXML là client để đáp ứng GUI môn học.
5. **`Entity` trừu tượng để làm gì?** Gom thuộc tính ID và hợp đồng `summary()`; subtype phải override hành vi của mình.
6. **Đâu là đa hình?** User tham chiếu loại trừu tượng vẫn trả `role()` của class con; `Item.category()` tương tự.
7. **Factory Method ở đâu?** `ItemFactory.create` là **Factory Method**: lớp `ItemFactory` khai báo phương thức `createItem()`, các lớp factory con override để tạo từng subtype.
8. **Singleton có phải ưu điểm tuyệt đối?** Không; tiện chia sẻ service instance trong một JVM nhưng làm test và scale khó. Constructor public được dùng để tạo instance độc lập trong test.
9. **Observer có khác polling?** Observer chủ động phát khi bid thành công; polling là client hỏi lại server theo chu kỳ. SSE giữ kết nối và đẩy event khi có thay đổi.
10. **SSE có phải TCP Socket?** SSE chạy trên HTTP được truyền bởi TCP; bản hiện tại không dùng `java.net.Socket` ứng dụng trực tiếp. Đề cho phép REST API hoặc Socket JSON.
11. **JSON có lợi thế gì?** Đa ngôn ngữ (JavaFX, browser), dễ debug, chỉ truyền DTO thay vì serialize object Java qua mạng.
12. **Race condition là gì?** Hai thread cùng đọc giá cũ và cùng ghi giá mới gây lost update hoặc hai winner.
13. **Tại sao `volatile` không đủ?** `volatile` đảm bảo visibility nhưng không biến thao tác đọc–so sánh–ghi thành atomic; cần lock/transaction.
14. **`synchronized` dùng ở đâu?** `AuctionService.bid`, các thao tác sửa và `transact` đều xảy ra trong synchronized method cùng instance.
15. **Hai người cùng đặt 11 triệu thì sao?** Giao dịch đầu thành công; giao dịch sau nhìn thấy currentPrice=11 triệu, trả lỗi 422; lịch sử chỉ chứa một bid ở giá này.
16. **Nếu ghi đĩa lỗi sau khi thay giá?** Service sửa bản copy trước. Nếu save lỗi, không thay `state` dùng để phục vụ và không notify; lỗi 500.
17. **Làm sao xác định người thắng?** Auction.leaderId luôn gắn với mức currentPrice mới nhất; ở trạng thái FINISHED, leaderId là winner hoặc null nếu không có bid.
18. **Tại sao thời gian dùng server time?** Client clock không đáng tin; start/end được kiểm tra bằng `clock.millis()` phía server.
19. **Nếu đặt giá đúng thời điểm hết phiên?** Quy tắc `now < endTime`: bằng hoặc sau endTime bị từ chối (HTTP 409), trừ khi anti-sniping đã gia hạn trước đó.
20. **Anti-sniping xử lý thế nào?** Nếu endTime−now≤30s, đặt endTime=max(endTime, now+60s); không giảm thời gian.
21. **Một auto-bid có thể vượt max không?** Không; chỉ thêm bid nếu giá hiện tại + increment ≤ maxAmount; có giới hạn 500 bước để tránh quá tải.
22. **SSE mất kết nối thì sao?** Browser EventSource tự reconnect, trạng thái chính thức vẫn nằm trên server; cần thêm GET snapshot lúc reconnect để robust hơn.
23. **Dữ liệu lưu ở đâu?** FileRepository serialized data hoặc SqliteRepository BLOB có JDBC. Không phải schema quan hệ chuẩn hóa; chọn hướng này để đơn giản hóa môn học.
24. **Password có lưu plaintext?** Không, lưu salt và PBKDF2-HMAC-SHA256 hash; demo fixture mật khẩu công khai chỉ bật khi `AUCTION_DEMO=1`.
25. **Đồ án này đủ production chưa?** Chưa. Cần HTTPS, rate limiting, audit logs, validation mạnh hơn, schema SQL normalized, session revoke toàn diện, monitoring, deployment nhiều instance và test tải.

## Câu hỏi phản biện nâng cao

**Nếu có 10.000 client SSE?** Mỗi SSE client dùng một virtual thread và queue giới hạn; tại quy mô lớn cần message broker, reverse proxy, bounded memory/connection governance, backpressure và load testing.

**Nếu hai server cùng viết một SQLite file?** Không được bảo đảm tính nhất quán bởi `synchronized` giữa các JVM. Thiết kế này chỉ dành cho một server process; production cần DB transaction optimistic locking/row locks xuyên tiến trình, unique invariants, event broker.

**Nếu client sửa JSON `price` trên Developer Tools?** Không thể bypass server, vì server kiểm tra từ token, vai trò, timestamp và currentPrice thực tế.

**Biểu đồ có phản ánh đúng bid không?** Xét dữ liệu server: trục X là thứ tự các BidTransaction, trục Y là amount. Không cập nhật từ giá client tự tính; cập nhật sau sự kiện từ server.

## Những gì tuyệt đối không được trả lời sai

- Không nói “SSE là WebSocket”; chúng là hai cơ chế khác nhau.
- Không nói “SQLite luôn được dùng mặc định”; mode mặc định hiện là **file serialization**.
- Không nói “cứ dùng synchronized là hỗ trợ nhiều JVM”; synchronized chỉ bảo vệ một instance JVM.
- Không nói “auto-bid tối ưu độ phức tạp”; dù đã dùng PriorityQueue chọn ứng viên, thuật toán hiện vẫn giới hạn tối đa 500 vòng.
- Không nói “mã hóa mật khẩu”; đây là **hash có salt**, không phải reversible encryption.
- Không nói “đã chạy JUnit trên CI” khi chưa tự chạy trên GitHub.
