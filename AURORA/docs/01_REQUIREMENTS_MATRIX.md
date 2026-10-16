# Đối chiếu yêu cầu & thang điểm — LTNC 2026

Nguồn chính: **2026-BTL.pdf** (trang 3–10), **Hướng dẫn BTL LTNC-2026.pdf** (trang 1–7). Mục tiêu: chứng minh chức năng bằng **code + demo + test**, không chỉ slide.

| Yêu cầu đề bài | Vị trí triển khai | Bằng chứng khi bảo vệ | Đánh giá hiện tại |
|---|---|---|---|
| User abstract; Bidder/Seller/Admin | `model/User.java`, `Bidder`, `Seller`, `Admin` | Phân cấp `User extends Entity`; role-based access | Đã viết; chạy smoke test |
| Item abstract; Electronics/Art/Vehicle | `model/Item.java`, 3 class con | Gọi `ItemFactory.create()` và `item.category()` đa hình | Đã viết |
| Đóng gói, trừu tượng, đa hình, kế thừa | `model/` | Mô tả private field/getter, abstract class, override | Đã viết |
| Singleton, Factory, Observer | `AuctionService.singleton`, `ItemFactory`, `Events` | Vẽ sequence bid → observer → clients | Đã viết |
| CRUD sản phẩm | `AuctionService.create/edit/delete`, REST endpoints | Demo Seller thêm/sửa/xóa (trước khi có bid) | Đã viết |
| Bid hợp lệ, cập nhật cao nhất | `AuctionService.bid()` | Bid thấp hơn bị HTTP 422 | Đã viết, smoke test |
| Trạng thái OPEN/RUNNING/FINISHED/PAID/CANCELED | `Auction.Status`, `tick()`, `pay()`, `cancel()` | Scheduler 500ms, PAID sau FINISHED | Đã viết; cần demo time |
| Lỗi nhập liệu/kết nối | `ServiceException`, HTTP Controller, UI notifications | 400/401/403/404/409/422/500 | Đã viết |
| Race condition / lost update | `AuctionService.transact()` synchronized; clone-save-publish | Hai người cùng bid 3.000 → một thành công | Đã kiểm bằng standalone smoke test |
| Realtime observer/socket | `Events.java`, HTTP SSE /api/events | Client A bid → client B nhận EventSource push | Đã viết; cần demo 2 client trực tiếp |
| Client–Server / REST JSON | `ApiServer`, `ApiClient` | Không client nào ghi storage trực tiếp | Đã viết, HTTP smoke test |
| JavaFX + FXML / MVC | `client-javafx/`, `dashboard.fxml`, `DashboardController` | Demo client với `mvn javafx:run` | Đã viết nhưng **chưa build JavaFX được tại môi trường offline** |
| Maven / coding conventions | `pom.xml` (multi-module) | `mvn verify` | Chưa chạy Maven do công cụ không cài/mạng bị khóa |
| JUnit 5 | `AuctionServiceTest.java` | Unit tests và concurrency assertions | Đã viết; JUnit cần Maven tải dependency |
| GitHub Actions CI | `.github/workflows/ci.yml` | Build/test trên repo thật | Workflow đã viết, **chưa chạy trên GitHub** |
| Auto-bidding (maxBid, increment) | `setAuto()`, `resolveAuto()` | Bid trả tự động theo quy tắc | Đã viết, smoke test |
| Anti-sniping | `addBid()` | Bid trong 30s cuối → endTime ≥ now+60s | Đã viết, JUnit test |
| Bid History Line Chart realtime | Web `buildChart()`, JavaFX `LineChart` | Đặt bid và xem đường giá biến đổi | Đã viết |

**Chú ý:** các đầu mục ghi “Đã viết” không thay cho bằng chứng đã kiểm thử end-to-end. Chạy theo script demo trước buổi chấm điểm.
