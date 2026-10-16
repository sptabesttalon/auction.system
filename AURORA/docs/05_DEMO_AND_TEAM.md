# Kế hoạch demo, phân việc nhóm, chiến lược bảo vệ

## 1. Demo trong 8–12 phút

1. (0:00–1:00) Trình bày kiến trúc (architecture.svg); hai client, một server, storage.
2. (1:00–2:00) Sơ đồ lớp (classes.svg); giải thích `Entity`/`User`/`Item`, Factory, Observer.
3. (2:00–4:00) Website: Seller đăng nhập/tạo sản phẩm. Mở thêm trình duyệt private với Bidder A, cửa sổ private khác Bidder B.
4. (4:00–6:00) Demo đặt giá: A bid 1.100.000, B đặt 1.200.000; mở cùng sản phẩm ở cả hai browser; cập nhật không cần F5.
5. (6:00–7:00) Auto-bid: A đặt max 1.500.000, bước 10.000; B bid; A tự trả tiếp trong giới hạn.
6. (7:00–8:30) Xem lịch sử bid và LineChart; giải thích đường giá chỉ đi lên.
7. (8:30–10:00) Chuyển đến client JavaFX, đăng nhập, chọn sản phẩm: cùng dữ liệu và realtime.
8. (10:00–12:00) Test cạnh tranh: chạy `mvn test` hoặc `scripts/test-core.sh`, giải thích concurrent equal bids. Nếu cần mô tả sơ đồ transaction.

**Anti-sniping demo nhanh:** tạo một phiên ngắn 1–2 phút (client JavaFX cho nhập thời lượng bằng phút), khi còn 30 giây, Bidder đặt giá; thời gian kết thúc tăng thêm ít nhất 60 giây. Không dùng chỉnh đồng hồ hệ điều hành khi demo.

## 2. Phân công gợi ý cho nhóm 4 thành viên

| Thành viên | Trách nhiệm chính | Cần hiểu phần còn lại |
|---|---|---|
| A | Model, OOP, Factory, User auth | Transaction, network, UI, testing |
| B | AuctionService, concurrency, auto-bid, timer | Model, REST, DB, UI |
| C | JavaFX/FXML, browser, usability, SSE | Server rules, DAO, test |
| D | Repository, Maven, JUnit, CI, documentation/demo | GUI, auction logic, architecture |

Yêu cầu môn học rất nghiêm: **mọi thành viên phải hiểu toàn bộ code**. Sau khi phân công, thực hiện review chéo; không giới hạn hiểu biết theo “phần của em”.

## 3. Commit log thật trên GitHub

Không tạo commit giả với thời gian quá khứ. Hãy thực hiện lịch làm thật, ví dụ:

```text
feat(model): add User and Item hierarchies
feat(server): add authentication and CRUD endpoints
feat(bidding): introduce synchronized bid transactions
feat(realtime): broadcast auction events over SSE
feat(gui): build JavaFX FXML screen
feat(web): create browser auction dashboard
feat(advanced): add proxy bidding and anti-sniping
fix(validation): reject bids on closed auctions
test(bidding): cover concurrent equal-price requests
docs(defense): explain diagrams and demo procedure
```

Tạo branches `main`, `dev`, feature branches; commit theo tính năng và làm Pull Request, tránh một commit duy nhất.

## 4. Trước ngày bảo vệ

- [ ] Java 21, Maven nhận trong PowerShell; `mvn -version` đúng version.
- [ ] Server chạy từ folder dự án không lỗi (`mvn -pl server exec:java`).
- [ ] Browser và JavaFX kết nối được cùng server.
- [ ] Có dữ liệu demo đã tạo sẵn nhưng vẫn trình diễn được tạo mới trực tiếp.
- [ ] JUnit `mvn test` xanh trên máy và CI xanh trên GitHub.
- [ ] Kiểm thử 2 bidder cùng giá và lỗi HTTP 422, log rõ ràng.
- [ ] Chụp ảnh/video demo làm phương án dự phòng nếu sự cố mạng.
- [ ] Mọi người có thể giải thích service, model, DAO, network, MVC và JUnit.
- [ ] Không dùng tài khoản demo công khai cho ứng dụng public.
