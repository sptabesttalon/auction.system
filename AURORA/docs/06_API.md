# Giao thức REST — hợp đồng giữa client và server

Host mặc định: `http://127.0.0.1:8080`. JSON UTF-8. Với API cần đăng nhập, gửi `Authorization: Bearer <token>`.

| Method | URL | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/health` | Không | Trạng thái HTTP server |
| POST | `/api/auth/register` | Không | Đăng ký BIDDER / SELLER |
| POST | `/api/auth/login` | Không | Trả token + profile |
| POST | `/api/auth/logout` | Có | Hủy token |
| GET | `/api/me` | Có | Người dùng hiện tại |
| GET | `/api/auctions` | Không | Danh sách và giá hiện tại |
| POST | `/api/auctions` | SELLER/ADMIN | Tạo phiên |
| GET | `/api/auctions/{id}` | Không | Chi tiết, history |
| PUT | `/api/auctions/{id}` | Chủ sở hữu/ADMIN | Sửa tên và mô tả trước khi có bid |
| DELETE | `/api/auctions/{id}` | Chủ sở hữu/ADMIN | Xóa phiên chưa bid/chưa hoàn thành |
| POST | `/api/auctions/{id}/bid` | BIDDER | Đặt giá thủ công |
| POST | `/api/auctions/{id}/auto` | BIDDER | Đăng ký auto-bid |
| POST | `/api/auctions/{id}/cancel` | Seller/ADMIN | Hủy phiên OPEN/RUNNING/FINISHED theo quyền; không hủy PAID |
| POST | `/api/auctions/{id}/pay` | Seller/ADMIN | Đánh dấu đã thanh toán |
| GET | `/api/events` | Public | SSE thông báo thay đổi (stream) |

## Ví dụ đăng ký / đăng nhập

```bash
curl -X POST http://127.0.0.1:8080/api/auth/register -H 'Content-Type: application/json' -d '{"username":"demo_bidder","password":"password123","role":"BIDDER"}'
curl -X POST http://127.0.0.1:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"demo_bidder","password":"password123"}'
```

Phản hồi login dạng:

```json
{"token":"RANDOM_BEARER_TOKEN","user":{"id":1,"username":"demo_bidder","role":"BIDDER"}}
```

## Ví dụ tạo phiên

`startPrice`, `startTime`, `endTime` sử dụng **số nguyên**. Timestamps tính bằng **milliseconds since Unix epoch** (không phải giây).

```json
{
  "category":"ELECTRONICS",
  "title":"MacBook Air M2",
  "description":"RAM 16GB, SSD 512GB, máy đẹp",
  "startPrice":15000000,
  "startTime":1800000000000,
  "endTime":1800003600000
}
```

## Ví dụ đặt giá

```http
POST /api/auctions/1/bid
Authorization: Bearer YOUR_TOKEN
Content-Type: application/json

{"amount":16000000}
```

## Auto-bid

```json
{"maxAmount":20000000,"increment":100000}
```

## Realtime event

```text
GET /api/events
Accept: text/event-stream

: connected

data: {"type":"bid_placed","time":1800000000000}

: heartbeat
```

Event không phải dữ liệu giá chính thức, mà là tín hiệu để client fetch lại snapshot. Mặc định heartbeat mỗi 15 giây giúp giữ kết nối.

## Mã lỗi

- `400` định dạng request, trường bắt buộc, giới hạn đầu vào
- `401` sai username/password, hết hạn token, chưa đăng nhập
- `403` vai trò/ownership không hợp lệ
- `404` auction hoặc endpoint không tồn tại
- `409` đặt giá khi đã đóng
- `422` bid bằng/thấp hơn currentPrice hoặc cấu hình auto-bid sai
- `500` persistence/server issue

Khi lỗi, body có `{ "error": "thông báo tiếng Việt" }`.
