#!/usr/bin/env python3
"""Generate a Vietnamese A4 project-defense handbook with embedded architecture diagrams."""
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Image, KeepTogether
from reportlab.lib.pagesizes import A4
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from PIL import Image as PILImage

BASE = Path(__file__).resolve().parent.parent
TARGET = BASE/'docs'/'BAO_CAO_BAO_VE.pdf'
PAGE_W,PAGE_H=A4
NAVY=HexColor('#112133');ORANGE=HexColor('#ec8150');INK=HexColor('#213345');LIGHT=HexColor('#f3f5f7');GRAY=HexColor('#677789');WHITE=HexColor('#ffffff')
pdfmetrics.registerFont(TTFont('DejaVu','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'))
pdfmetrics.registerFont(TTFont('DejaVu-Bold','/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'))
styles={
    'h1':ParagraphStyle('h1',fontName='DejaVu-Bold',fontSize=19,leading=27,textColor=NAVY,spaceAfter=16),
    'h2':ParagraphStyle('h2',fontName='DejaVu-Bold',fontSize=13.5,leading=20,textColor=NAVY,spaceBefore=12,spaceAfter=9),
    'p':ParagraphStyle('p',fontName='DejaVu',fontSize=9.6,leading=16.6,textColor=INK,spaceAfter=8),
    'small':ParagraphStyle('small',fontName='DejaVu',fontSize=8.6,leading=14.5,textColor=GRAY,spaceAfter=9),
    'bullet':ParagraphStyle('bullet',fontName='DejaVu',fontSize=9.2,leading=16.4,textColor=INK,leftIndent=15,firstLineIndent=-10,spaceAfter=6),
    'table':ParagraphStyle('table',fontName='DejaVu',fontSize=8.2,leading=12.7,textColor=INK),
    'tablehead':ParagraphStyle('tablehead',fontName='DejaVu-Bold',fontSize=8.6,leading=13,textColor=WHITE),
    'quote':ParagraphStyle('quote',fontName='DejaVu',fontSize=10,leading=17,textColor=INK,leftIndent=17,rightIndent=17,spaceAfter=13),
    'center':ParagraphStyle('center',fontName='DejaVu-Bold',fontSize=10,leading=16,textColor=NAVY,alignment=TA_CENTER,spaceAfter=10),
}

def P(text,style='p'):
    return Paragraph(text,styles[style])

def H(title): return P(escape(title),'h1')

def H2(title): return P(escape(title),'h2')

def bullet(text):return P('• '+text,'bullet')

def diagram(name,caption):
    png=BASE/'docs'/'diagrams'/f'{name}.png'
    im=PILImage.open(png)
    scale=min((PAGE_W-46*mm)/im.width,(PAGE_H-107*mm)/im.height)
    image=Image(str(png),width=im.width*scale,height=im.height*scale)
    image.hAlign='CENTER'
    return [P(escape(caption),'small'),Spacer(1,8),image]

def add_table(rows,widths=None):
    prepared=[]
    for i,row in enumerate(rows):
        prepared.append([P(escape(str(col)),'tablehead' if i==0 else 'table') for col in row])
    t=Table(prepared,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND',(0,0),(-1,0),NAVY),('ROWBACKGROUNDS',(0,1),(-1,-1),[WHITE,LIGHT]),
        ('VALIGN',(0,0),(-1,-1),'TOP'),('BOTTOMPADDING',(0,0),(-1,-1),9),('TOPPADDING',(0,0),(-1,-1),9),
        ('LEFTPADDING',(0,0),(-1,-1),10),('RIGHTPADDING',(0,0),(-1,-1),10),
        ('LINEBELOW',(0,-1),(-1,-1),.7,HexColor('#e0e4e8'))]))
    return t

def cover(cv,doc):
    cv.saveState();cv.setFillColor(NAVY);cv.rect(0,0,PAGE_W,PAGE_H,fill=1,stroke=0)
    cv.setFillColor(HexColor('#263a50'));cv.circle(PAGE_W-25*mm,PAGE_H-70*mm,70*mm,fill=1,stroke=0)
    cv.setFillColor(HexColor('#3a5367'));cv.circle(PAGE_W-40*mm,PAGE_H-82*mm,47*mm,fill=1,stroke=0)
    cv.setFillColor(ORANGE);cv.roundRect(27*mm,PAGE_H-40*mm,15*mm,15*mm,4*mm,fill=1,stroke=0)
    cv.setFillColor(NAVY);cv.setFont('DejaVu-Bold',21);cv.drawCentredString(34.5*mm,PAGE_H-35.9*mm,'✦')
    cv.setFillColor(HexColor('#f4f1eb'));cv.setFont('DejaVu-Bold',19);cv.drawString(49*mm,PAGE_H-34*mm,'AURORA.')
    cv.setFillColor(HexColor('#ffbb94'));cv.setFont('DejaVu-Bold',10);cv.drawString(27*mm,PAGE_H-92*mm,'TÀI LIỆU THIẾT KẾ VÀ BẢO VỆ ĐỒ ÁN')
    cv.setFillColor(WHITE);cv.setFont('DejaVu-Bold',31);cv.drawString(27*mm,PAGE_H-113*mm,'HỆ THỐNG')
    cv.drawString(27*mm,PAGE_H-132*mm,'ĐẤU GIÁ')
    cv.drawString(27*mm,PAGE_H-151*mm,'TRỰC TUYẾN')
    cv.setFillColor(HexColor('#ffae7d'));cv.setFont('DejaVu-Bold',13);cv.drawString(27*mm,PAGE_H-169*mm,'JAVA 21  •  JAVAFX  •  REST / SSE  •  MVC')
    cv.setStrokeColor(HexColor('#687b87'));cv.line(27*mm,PAGE_H-185*mm,PAGE_W-27*mm,PAGE_H-185*mm)
    cv.setFont('DejaVu',11);cv.setFillColor(HexColor('#c0d1dd'))
    cv.drawString(27*mm,PAGE_H-198*mm,'Bài tập lớn Lập trình nâng cao • UET.CS2043')
    cv.drawString(27*mm,PAGE_H-210*mm,'Kiến trúc • OOP • Concurrency • Observer • Kiểm thử')
    cv.setFillColor(HexColor('#97abba'));cv.setFont('DejaVu',9)
    cv.drawString(27*mm,34*mm,'Bản tài liệu học và tập bảo vệ — cần kiểm thử đầy đủ trước khi nộp')
    cv.drawString(27*mm,26*mm,'Năm 2026 • AURORA Online Auction System')
    cv.restoreState()

def page_frame(cv,doc):
    cv.saveState()
    cv.setFillColor(NAVY);cv.rect(0,PAGE_H-17*mm,PAGE_W,17*mm,fill=1,stroke=0)
    cv.setFillColor(ORANGE);cv.setFont('DejaVu-Bold',10);cv.drawString(23*mm,PAGE_H-11.4*mm,'✦ AURORA')
    cv.setFillColor(WHITE);cv.setFont('DejaVu',8.5);cv.drawRightString(PAGE_W-23*mm,PAGE_H-11.4*mm,'UET.CS2043  •  TÀI LIỆU BẢO VỆ')
    cv.setStrokeColor(HexColor('#dddddf'));cv.line(23*mm,17*mm,PAGE_W-23*mm,17*mm)
    cv.setFillColor(GRAY);cv.setFont('DejaVu',8);cv.drawString(23*mm,11*mm,'Java • MVC • Client–Server • Concurrency')
    cv.drawRightString(PAGE_W-23*mm,11*mm,str(doc.page))
    cv.restoreState()

story=[Spacer(1,600),PageBreak()]
story += [H('1. Tổng quan đồ án'),P('<b>AURORA</b> là hệ thống đấu giá trực tuyến đa client. Website và ứng dụng JavaFX cùng gọi REST API đến một Java Server. Người dùng được phân vai Bidder, Seller và Admin. Máy chủ là nguồn dữ liệu giá duy nhất.'),
H2('Mục tiêu theo đề môn học'),bullet('<b>OOP:</b> abstract Entity, User, Item; kế thừa, đóng gói, đa hình, trừu tượng.'),bullet('<b>MVC:</b> JavaFX + FXML, Server Controller → Model/Service → Repository (DAO).'),bullet('<b>Concurrency:</b> tránh lost update, giá rollback và hai người thắng cùng lúc.'),bullet('<b>Realtime:</b> Observer / SSE phát sự kiện đến mọi client theo dõi.'),bullet('<b>Chất lượng:</b> Maven, JUnit 5, GitHub Actions, xử lý ngoại lệ.'),
H2('Nhóm tính năng có trong mã nguồn'),add_table([['Nhóm','Chức năng'],['Tài khoản','Đăng ký, đăng nhập, phân vai Bidder/Seller/Admin'],['Đấu giá','Tạo/sửa/xóa sản phẩm, đặt giá, lưu history, auto close'],['Nâng cao','Auto-bid, anti-sniping, biểu đồ giá cập nhật theo event'],['Hệ thống','REST/JSON, SSE, File Serialization, tùy chọn SQLite JDBC']],[43*mm,119*mm]),
Spacer(1,10),P('<b>Lưu ý:</b> Server và HTTP core đã được biên dịch, chạy kiểm thử. JavaFX/Maven/CI cần được chạy xác nhận trên máy có đầy đủ thư viện và Internet.','small'),PageBreak()]
for i,(name,title,caption) in enumerate([
    ('architecture','2. Kiến trúc Client–Server','Hai giao diện độc lập chia sẻ một server. Mọi giá đấu và lưu trữ do server quản lý.'),
    ('layers','3. Kiến trúc logic MVC / DAO','Sơ đồ cho thấy tách riêng hiển thị, điều khiển, nghiệp vụ và dữ liệu.'),
    ('classes','4. Sơ đồ lớp OOP','Kế thừa từ Entity/User/Item, composition trong Auction, hỗ trợ Factory Method.'),
    ('states','5. Máy trạng thái phiên đấu giá','OPEN → RUNNING → FINISHED → PAID; CANCELED dùng khi được phép hủy.'),
    ('bid-sequence','6. Sơ đồ tuần tự một lượt bid','Client gửi request; server khóa giao dịch, lưu thành công mới công bố sự kiện.'),
    ('transaction','7. Sơ đồ logic đảm bảo atomicity','Clone → validate → persist → publish bảo toàn giá khi kiểm tra hoặc ghi dữ liệu thất bại.'),
]):
    story += [H(title),*diagram(name,caption),PageBreak()]
story += [H('8. Thuật toán cạnh tranh giá đồng thời'),
 P('Giả sử giá đang là <b>10.000.000 VND</b>. Bidder A và B cùng đặt <b>11.000.000 VND</b>. Nếu cả hai thread cùng đọc giá cũ rồi ghi đè thì dẫn đến <i>lost update</i> hoặc báo hai người thắng.'),
 H2('Quy trình trong AuctionService.bid()'),
 bullet('<b>1.</b> Java khóa một service instance qua synchronized.'),
 bullet('<b>2.</b> Thread A lấy state hiện tại, sao chép sang state tạm.'),
 bullet('<b>3.</b> A hợp lệ, ghi một BidTransaction, currentPrice=11 triệu, leaderId=A.'),
 bullet('<b>4.</b> Repository lưu state mới, sau đó service mới thay state chính thức và gửi Observer event.'),
 bullet('<b>5.</b> B vào sau và đọc currentPrice=11 triệu nên bid bằng 11 triệu bị từ chối (HTTP 422).'),
 H2('Bất biến (invariants) cần bảo vệ'),
 bullet('currentPrice của các bid đã chấp nhận phải tăng <b>nghiêm ngặt</b>.'),
 bullet('Tại một thời điểm Auction chỉ có duy nhất một leaderId.'),
 bullet('Tuyệt đối không notify client giá thành công trước khi dữ liệu đã lưu.'),
 bullet('Một giao dịch lỗi phải để state chính thức không thay đổi.'),
 H2('Giới hạn kiến trúc'),P('Khóa synchronized chỉ áp dụng trong một JVM, không phải khóa phân tán. Khi mở rộng nhiều server cần DB row locking/optimistic locking; ở bản môn học ưu tiên một server đảm bảo tính đúng.'),
 PageBreak()]
story += [H('9. Auto-bidding • Anti-sniping • Realtime'),
 H2('Auto-bidding'),P('Người dùng đặt <b>maxAmount</b> và <b>increment</b>. Server lưu một AutoBid và trong mỗi vòng tạo PriorityQueue xếp theo max giảm dần, timestamp đăng ký tăng dần. Các rule của người đang dẫn đầu được loại khỏi lượt phản hồi; chỉ chấp nhận giá mới nếu currentPrice+increment ≤ maxAmount. Mỗi bid tự động được ghi lịch sử với automatic=true.'),
 P('<b>Ví dụ:</b> A đăng ký max=8.000, bước=1.000; B trả giá 3.000; server tự đặt 4.000 cho A. Khi B đặt 9.000, rule A không còn đủ ngân sách và B dẫn đầu. Thuật toán bản mẫu chặn ở 500 bước để tránh vòng lặp quá dài.'),
 H2('Anti-sniping'),P('Trong 30 giây cuối, một bid hợp lệ sẽ kéo dài thời hạn đến <b>ít nhất</b> currentServerTime+60 giây. Ví dụ end=20:00:00, bid=19:59:50 ⇒ end tối thiểu 20:00:50. Dùng Math.max để không bao giờ rút ngắn thời gian.'),
 H2('Realtime Observer'),P('Observer trong AuctionService phát sự kiện <b>chỉ sau khi commit</b>. SSE giữ một HTTP connection; trình duyệt nhận qua EventSource, JavaFX đọc stream nền. Sau event, client GET snapshot mới để hiển thị giá và vẽ biểu đồ. Đếm ngược mỗi giây chỉ là thay đổi UI local, không phải polling server.'),
 H2('Bảo mật ở mức học phần'),P('Hash mật khẩu PBKDF2-HMAC-SHA256 với salt ngẫu nhiên; Bearer token thời hạn 24 giờ, RBAC tại server. Chưa có HTTPS/rate-limit/production hardening, không chạy công khai trên Internet.'),PageBreak()]
story += [H('10. Kịch bản demo thực tế (8–12 phút)'),
 add_table([
 ['Thời gian','Thao tác demo','Ý nghĩa'],
 ['0:00–2:00','Giới thiệu sơ đồ kiến trúc và cây kế thừa.','Giải thích Client–Server, OOP, MVC.'],
 ['2:00–4:00','Seller đăng nhập, thêm/sửa sản phẩm.','CRUD, role, validation.'],
 ['4:00–6:00','2 Bidder ở 2 trình duyệt cùng mở phiên và tăng giá.','Realtime push, lịch sử, kiểm tra giá.'],
 ['6:00–7:30','Bật auto-bid; một bidder khác trả giá.','Max/increment, PriorityQueue.'],
 ['7:30–9:00','Xem biến động giá trên chart; mở thêm JavaFX.','Hai client dùng chung REST server.'],
 ['9:00–12:00','Chạy smoke/JUnit test cạnh tranh, demo anti-sniping.','Không lost update; timer trên server.'],
 ],[28*mm,88*mm,46*mm]),
 Spacer(1,18),H2('Các lệnh cần thuộc'),
 P('<b>Server:</b> <font face="DejaVu">mvn -pl server exec:java</font>'),
 P('<b>JavaFX:</b> <font face="DejaVu">mvn -pl client-javafx javafx:run</font>'),
 P('<b>JUnit:</b> <font face="DejaVu">mvn test</font>'),
 P('<b>Core smoke khi thiếu Maven:</b> <font face="DejaVu">bash scripts/test-core.sh</font>'),
 P('<b>HTTP end-to-end test:</b> <font face="DejaVu">python scripts/test-http.py</font>'),
 PageBreak()]
qa=[
('1. MVC khác OOP như thế nào?','MVC chia trách nhiệm kiến trúc giữa Model/View/Controller; OOP tổ chức các đối tượng dùng kế thừa, đóng gói, trừu tượng, đa hình.'),
('2. Vì sao client không tự tính giá?','Người dùng có thể sửa client. Giá chính thức phải kiểm tra ở server dùng dữ liệu nhất quán.'),
('3. synchronized có đủ cho hai server không?','Không; synchronized chỉ khóa một JVM. Multi-instance cần khóa DB hoặc giao dịch phân tán.'),
('4. Tại sao volatile không đủ?','Volatile cung cấp visibility nhưng không atomic cho chuỗi read–check–write.'),
('5. Nếu hai bidder cùng đưa một giá?','Chỉ một giao dịch giữ khóa trước được chấp nhận; giao dịch sau gặp giá không lớn hơn hiện tại.'),
('6. Vì sao clone state trước khi ghi?','Nếu validation hoặc repository.save lỗi, state đang phục vụ không bị thay đổi.'),
('7. Observer khác polling ra sao?','Observer phát event chủ động sau commit; polling gửi GET theo chu kỳ dù không có thay đổi.'),
('8. SSE và WebSocket có giống nhau không?','Không. SSE là push một chiều server→client trên HTTP, đủ cho thông báo bid; gửi bid dùng POST REST.'),
('9. Thời gian lấy từ đâu?','Dựa vào Clock của server; client clock chỉ dùng hiển thị.'),
('10. Làm thế nào chọn winner?','Auction.leaderId cập nhật sau bid thành công, giữ duy nhất một người dẫn đầu khi FINISHED.'),
('11. Tại sao dùng Factory Method?','Các concrete creator override createItem để tạo Electronics/Art/Vehicle, tách logic tạo đối tượng khỏi nghiệp vụ.'),
('12. Có thể sửa sản phẩm sau khi có bid không?','Không, bản này chỉ cho phép sửa trước khi có bid để tránh đổi thông tin sản phẩm sau khi người dùng quyết định giá.'),
('13. Auto-bid có thể vượt max không?','Không; mỗi candidate phải thỏa currentPrice+increment ≤ maxAmount.'),
('14. Tại sao phải anti-sniping?','Tránh việc bidder đặt giá vào những giây cuối khiến người khác không có cơ hội phản hồi.'),
('15. Dữ liệu có lưu password gốc không?','Không: PBKDF2-HMAC-SHA256 + salt; chỉ lưu passwordHash/salt.'),
('16. Cần cải tiến gì nếu deploy Internet?','HTTPS, rate-limit, auditing, normalized DB, monitoring, backups, production authentication/session store.'),
]
story += [H('11. Câu hỏi phản biện thường gặp')]
for q,a in qa:
    story.append(KeepTogether([P(escape(q),'h2'),P(escape(a))]))
story += [PageBreak(),H('12. Tình trạng kiểm thử và phạm vi trung thực'),
 H2('Đã xác nhận trong môi trường xây dựng'),
 bullet('Java 21: javac biên dịch server thành công.'),
 bullet('scripts/test-core.sh: pass đăng nhập/phân quyền/đặt giá/concurrent/auto/JSON.'),
 bullet('scripts/test-http.py: pass HTTP register/login, seller create, giá không hợp lệ, hai request cùng giá, auto-bid và SSE push.'),
 H2('Chưa xác nhận trong môi trường xây dựng'),
 bullet('Maven, JUnit 5 suite, GitHub Actions: không cài Maven và không thể tải dependencies.'),
 bullet('JavaFX client: đã viết FXML/controller nhưng chưa compile/run giao diện vì thiếu JavaFX runtime.'),
 bullet('SQLite JDBC: có mã nguồn nhưng chưa chạy thực do thiếu sqlite-jdbc jar.'),
 bullet('Web UI thao tác trực tiếp trên trình duyệt: đã có mã nguồn và phục vụ HTML, nhưng môi trường không cho browser automation truy cập localhost.'),
 H2('Lưu ý theo đề bài'),P('Đề môn học yêu cầu commit GitHub đều đặn và các thành viên hiểu mọi phần mã nguồn. Không giả lập lịch sử commit; nhóm phải thật sự học code, thực hiện review chéo và chạy kiểm thử của chính mình trước khi trình bày.'),
 Spacer(1,18),P('<b>Tài liệu chi tiết hơn:</b> Xem README.md và docs/*.md trong ZIP. Mọi sơ đồ có phiên bản .dot để chỉnh sửa và .svg/.png để đưa vào slide.'),
]

doc=SimpleDocTemplate(str(TARGET),pagesize=A4,leftMargin=23*mm,rightMargin=23*mm,topMargin=25*mm,bottomMargin=24*mm,title='AURORA — Tài liệu thiết kế và bảo vệ hệ thống đấu giá trực tuyến',author='UET Advanced Programming Project',pageCompression=1)
doc.build(story,onFirstPage=cover,onLaterPages=page_frame)
print(TARGET)
