'use strict';
const $=selector=>document.querySelector(selector);
const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const money=n=>new Intl.NumberFormat('vi-VN').format(n)+' ₫';
const shortDate=t=>new Intl.DateTimeFormat('vi-VN',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}).format(new Date(t));
const formatTime=t=>new Date(t).toLocaleString('vi-VN');
let token=localStorage.getItem('aurora_token')||'';
let user=null,auctions=[],detailId=null,statusFilter='ALL',keyword='',categoryFilter='ALL',liveVersion=0;
const names={ELECTRONICS:'CÔNG NGHỆ',ART:'NGHỆ THUẬT',VEHICLE:'XE CỘ'};
const statuses={RUNNING:'ĐANG DIỄN RA',OPEN:'SẮP BẮT ĐẦU',FINISHED:'ĐÃ KẾT THÚC',PAID:'ĐÃ THANH TOÁN',CANCELED:'ĐÃ HỦY'};
async function api(path,method='GET',body){
  const response=await fetch('/api'+path,{method,headers:{...(body?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});
  let result;try{result=await response.json();}catch{throw Error('Máy chủ trả dữ liệu không hợp lệ');}
  if(!response.ok){if(response.status===401&&path==='/me')logoutLocal();throw Error(result.error||'Yêu cầu thất bại');}
  return result;
}
function notify(message,error=false){
  const el=document.createElement('div');el.className='toast'+(error?' error':'');el.textContent=message;$('#toasts').append(el);setTimeout(()=>el.remove(),4000);
}
function logoutLocal(){token='';user=null;localStorage.removeItem('aurora_token');renderUser();}
function renderUser(){
  $('#userArea').innerHTML=user
    ?`<span class="profile-chip">${escapeHtml(user.username)} · ${escapeHtml(user.role)}</span>${user.role==='SELLER'||user.role==='ADMIN'?'<button class="btn btn-primary" data-action="create">+ Đăng sản phẩm</button>':''}<button class="btn btn-soft" data-action="logout">Đăng xuất</button>`
    :'<button class="btn btn-soft" data-action="login">Đăng nhập</button><button class="btn btn-primary" data-action="register">Tạo tài khoản ↗</button>';
}
function countdown(t){let secs=Math.max(0,Math.ceil((t-Date.now())/1000));const d=Math.floor(secs/86400);secs%=86400;let h=Math.floor(secs/3600);secs%=3600;let m=Math.floor(secs/60);let s=secs%60;return (d?d+' ngày ':'')+[h,m,s].map(v=>String(v).padStart(2,'0')).join(':');}
function statusText(a){return a.status==='RUNNING'?`Còn ${countdown(a.endTime)}`:a.status==='OPEN'?`Mở sau ${countdown(a.startTime)}`:statuses[a.status];}
function card(a){return `<article class="auction-card" data-id="${a.id}" tabindex="0" role="button" aria-label="Xem ${escapeHtml(a.title)}"><div class="card-picture"><span class="card-symbol">${escapeHtml(a.icon)}</span><span class="status-badge ${a.status.toLowerCase()}">${statuses[a.status]}</span><span class="card-id">#${String(a.id).padStart(4,'0')}</span></div><div class="card-body"><span class="card-category">${names[a.category]}</span><h3 class="card-title">${escapeHtml(a.title)}</h3><div class="card-description">${escapeHtml(a.description)}</div><div class="card-price-row"><div><small>GIÁ HIỆN TẠI</small><strong>${money(a.currentPrice)}</strong></div><div class="bid-number"><span>LƯỢT ĐẤU</span><strong>${a.bidCount} ↗</strong></div></div><div class="card-footer"><span>◷ ${escapeHtml(statusText(a))}</span><span class="view">XEM CHI TIẾT ↗</span></div></div></article>`;}
function renderAuctions(){
  const list=auctions.filter(a=>(statusFilter==='ALL'||a.status===statusFilter||(statusFilter==='FINISHED'&&['PAID','CANCELED'].includes(a.status)))&&(categoryFilter==='ALL'||a.category===categoryFilter)&&(`${a.title} ${a.description} ${a.seller}`.toLowerCase().includes(keyword)));
  $('#auctionGrid').innerHTML=list.length?list.map(card).join(''):'<div class="empty">Chưa tìm thấy phiên đấu giá phù hợp.<br><span class="text-small">Thử bộ lọc khác hoặc đăng sản phẩm đầu tiên.</span></div>';
  $('#statAuctions').textContent=auctions.length;
  $('#statLive').textContent=auctions.filter(a=>a.status==='RUNNING').length;
  $('#statBids').textContent=auctions.reduce((s,a)=>s+a.bidCount,0);
}
async function refresh(showError=true){try{auctions=await api('/auctions');renderAuctions();}catch(e){if(showError)notify(e.message,true);}}
function showModal(html){$('#modalBody').innerHTML=html;$('#modalLayer').hidden=false;document.body.style.overflow='hidden';}
function closeModal(){detailId=null;$('#modalLayer').hidden=true;$('#modalBody').innerHTML='';document.body.style.overflow='';}
function titleBlock(label,title,subtitle=''){return `<div class="eyebrow orange">${label}</div><h2>${title}</h2>${subtitle?`<p>${subtitle}</p>`:''}`;}
function showLogin(){detailId=null;showModal(titleBlock('CHÀO MỪNG TRỞ LẠI','Đăng nhập','Tiếp tục khám phá những phiên đấu giá hấp dẫn.')+`<form class="modal-form" id="loginForm"><label>Tên đăng nhập<input name="username" required minlength="3" autocomplete="username" autofocus></label><label>Mật khẩu<input type="password" name="password" required autocomplete="current-password"></label><button class="btn btn-primary btn-lg">Đăng nhập ↗</button></form><button class="modal-swap" data-action="register">Chưa có tài khoản? Đăng ký ngay →</button>`);}
function showRegister(){detailId=null;showModal(titleBlock('THAM GIA AURORA','Tạo tài khoản','Lựa chọn vai trò phù hợp để bắt đầu.')+`<form class="modal-form" id="registerForm"><label>Tên đăng nhập<input name="username" required minlength="3" maxlength="32" placeholder="Ví dụ: hai_anh07" autocomplete="username"></label><label>Mật khẩu (tối thiểu 8 ký tự)<input type="password" name="password" required minlength="8" autocomplete="new-password"></label><label>Vai trò<select name="role"><option value="BIDDER">Bidder — tham gia đấu giá</option><option value="SELLER">Seller — đăng và quản lý sản phẩm</option></select></label><button class="btn btn-primary btn-lg">Tạo tài khoản ↗</button></form><button class="modal-swap" data-action="login">Đã có tài khoản? Đăng nhập →</button>`);}
function showCreate(){if(!user){showLogin();notify('Đăng nhập Seller để đăng sản phẩm');return;}if(!['SELLER','ADMIN'].includes(user.role)){notify('Chỉ Seller/Admin được đăng sản phẩm',true);return;}
  detailId=null;
  const dt=new Date(Date.now()+60_000);const dt2=new Date(Date.now()+3600_000);const local=v=>new Date(v.getTime()-v.getTimezoneOffset()*60000).toISOString().slice(0,16);
  showModal(titleBlock('DÀNH CHO NGƯỜI BÁN','Tạo phiên đấu giá','Thời gian được hệ thống xử lý theo đồng hồ máy chủ.')+`<form id="createForm" class="modal-form"><label>Danh mục<select name="category"><option value="ELECTRONICS">Công nghệ</option><option value="ART">Nghệ thuật</option><option value="VEHICLE">Xe cộ</option></select></label><label>Tên sản phẩm<input name="title" required minlength="3" maxlength="120" placeholder="Ví dụ: Máy ảnh Fujifilm X100"></label><label>Mô tả<textarea name="description" required minlength="5" maxlength="2000" placeholder="Mô tả tình trạng, nguồn gốc, đặc điểm..."></textarea></label><label>Giá khởi điểm (VNĐ)<input name="startPrice" type="number" min="1000" step="1000" value="1000000" required></label><div class="form-two"><label>Bắt đầu<input name="startTime" type="datetime-local" value="${local(dt)}" required></label><label>Kết thúc<input name="endTime" type="datetime-local" value="${local(dt2)}" required></label></div><button class="btn btn-primary btn-lg">Đăng sản phẩm ↗</button></form>`);
}
function buildChart(history,startPrice,startTime){
  const values=[startPrice,...history.map(h=>h.amount)];
  const stamps=[startTime,...history.map(h=>h.createdAt)];
  const width=520,height=160,pad=28,min=Math.min(...values),max=Math.max(...values),range=Math.max(max-min,1);
  const from=Math.min(...stamps),to=Math.max(...stamps),duration=Math.max(1,to-from);
  const x=(t,i)=>stamps.length===1?pad+(i?width-pad*2:0):pad+((t-from)/duration)*(width-pad*2);
  const y=v=>height-pad-((v-min)/range)*(height-pad*2);
  const path=values.length===1?`M${pad},${y(startPrice)} L${width-pad},${y(startPrice)}`
    :values.map((v,i)=>`${i?'L':'M'}${x(stamps[i],i)},${y(v)}`).join(' ');
  const lastX=values.length===1?width-pad:x(stamps[stamps.length-1],stamps.length-1),lastY=y(values[values.length-1]);
  return `<svg class="bid-chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="Biểu đồ mức giá theo timestamp"><defs><linearGradient id="fade" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="#fb8953" stop-opacity=".35"/><stop offset="100%" stop-color="#fb8953" stop-opacity="0"/></linearGradient></defs><path d="M${pad},${height-pad}H${width-pad}" stroke="#496075" stroke-width="1"/><path d="${path} L${width-pad},${height-pad} L${pad},${height-pad}Z" fill="url(#fade)"/><path d="${path}" stroke="#ffa777" stroke-width="3" stroke-linejoin="round" fill="none"/><circle cx="${lastX}" cy="${lastY}" r="5" fill="#ffa777"/><text x="${pad}" y="${height-7}" font-size="10" fill="#b2c3cd">${escapeHtml(shortDate(from))}</text><text x="${width-pad}" y="${height-7}" text-anchor="end" font-size="10" fill="#b2c3cd">${escapeHtml(shortDate(to))}</text></svg>`;
}

async function openDetail(id){
  detailId=id;let a;
  try{a=await api('/auctions/'+id);}catch(e){notify(e.message,true);return;}
  if(detailId!==id)return;
  const bidder=user?.role==='BIDDER';
  const manager=user&&(user.role==='ADMIN'||user.id===a.sellerId);
  const bidForm=bidder&&a.status==='RUNNING'?`<h3>Đặt giá của bạn</h3><form class="modal-form inline-form" id="bidForm"><input aria-label="Giá đặt" name="amount" required type="number" step="1000" min="${a.currentPrice+1}" value="${Math.ceil((a.currentPrice+1000)/1000)*1000}"><button class="btn btn-primary">ĐẶT GIÁ ↗</button></form><div class="form-note">Giá phải lớn hơn mức hiện tại. Thao tác này có hiệu lực ngay.</div><h3>⚡ Đấu giá tự động</h3><form id="autoForm" class="modal-form"><div class="form-two"><label>Giá tối đa (VND)<input name="maxAmount" type="number" min="${a.currentPrice+1000}" step="1000" required></label><label>Bước giá (VND)<input name="increment" type="number" min="1000" step="1000" value="10000" required></label></div><button class="btn btn-ghost">Kích hoạt Auto-bid</button></form>`:bidder?'<p>Phiên đấu giá hiện không nhận thêm bid.</p>':'<p class="muted">Đăng nhập tài khoản Bidder để đặt giá.</p>';
  const management=manager?`<div class="detail-actions">${!a.bidCount&&['OPEN','RUNNING'].includes(a.status)?`<button class="btn btn-ghost" data-action="edit" data-id="${a.id}">Sửa</button><button class="btn btn-danger" data-action="delete" data-id="${a.id}">Xóa</button>`:''}${['OPEN','RUNNING'].includes(a.status)?`<button class="btn btn-danger" data-action="cancel" data-id="${a.id}">Hủy phiên</button>`:''}${a.status==='FINISHED'&&a.leaderId?`<button class="btn btn-primary" data-action="pay" data-id="${a.id}">Xác nhận thanh toán</button>`:''}</div>`:'';
  const rows=a.history.slice().reverse().map(h=>`<div class="bid-row"><div><strong>${escapeHtml(h.bidder)}</strong>${h.automatic?'<span>⚡ Auto-bid</span>':'<span>Đặt giá trực tiếp</span>'}</div><div><strong>${money(h.amount)}</strong><span>${shortDate(h.createdAt)}</span></div></div>`).join('');
  showModal(`<div class="detail-header"><div class="detail-art">${escapeHtml(a.icon)}</div><div><div class="eyebrow orange">${names[a.category]} · #${a.id}</div><h2>${escapeHtml(a.title)}</h2><p>${escapeHtml(a.description)}</p><div class="detail-price">${money(a.currentPrice)}</div><div class="detail-meta">Người bán: <b>${escapeHtml(a.seller)}</b> · Dẫn đầu: <b>${escapeHtml(a.leader||'Chưa có')}</b><br>Trạng thái: <b>${statuses[a.status]}</b> · ${a.bidCount} lượt đặt giá<br>Kết thúc: <b>${formatTime(a.endTime)}</b><br><span class="timer-label">${escapeHtml(statusText(a))}</span></div>${management}</div></div><div class="divider"></div><div class="detail-grid"><div>${bidForm}</div><div><h3>Biểu đồ giá trực tiếp</h3>${buildChart(a.history,a.startPrice,a.startTime)}<h3>Lịch sử trả giá (${a.history.length})</h3><div class="bid-history">${rows||'<p>Chưa có lượt đặt giá nào.</p>'}</div></div></div>`);
}
async function loginSubmit(form){let m=Object.fromEntries(new FormData(form));let result=await api('/auth/login','POST',m);token=result.token;user=result.user;localStorage.setItem('aurora_token',token);renderUser();closeModal();notify('Đăng nhập thành công, chào '+user.username+'!');}
async function action(action,id){
  if(action==='login')return showLogin();if(action==='register')return showRegister();if(action==='create')return showCreate();
  if(action==='logout'){try{await api('/auth/logout','POST');}catch{}logoutLocal();notify('Đã đăng xuất');return;}
  if(['delete','cancel','pay'].includes(action)){
    let sentence=action==='delete'?'Xóa vĩnh viễn phiên đấu giá?':action==='cancel'?'Hủy phiên đấu giá này?':'Xác nhận người thắng đã thanh toán?';
    if(!confirm(sentence))return;
    await api('/auctions/'+id+(action==='delete'?'':'/'+action),action==='delete'?'DELETE':'POST');
    closeModal();await refresh();notify('Thao tác thành công');return;
  }
  if(action==='edit'){
    const a=await api('/auctions/'+id);
    showModal(titleBlock('QUẢN LÝ SẢN PHẨM','Sửa sản phẩm')+`<form id="editForm" class="modal-form" data-id="${id}"><label>Tên sản phẩm<input name="title" required minlength="3" maxlength="120" value="${escapeHtml(a.title)}"></label><label>Mô tả<textarea name="description" required minlength="5">${escapeHtml(a.description)}</textarea></label><button class="btn btn-primary">Lưu thay đổi</button></form>`);
  }
}
document.addEventListener('click',async e=>{
  const close=e.target.closest('[data-close]');if(close){closeModal();return;}
  const act=e.target.closest('[data-action]');if(act){try{await action(act.dataset.action,Number(act.dataset.id));}catch(error){notify(error.message,true);}return;}
  const cardEl=e.target.closest('.auction-card');if(cardEl)await openDetail(Number(cardEl.dataset.id));
});
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!$('#modalLayer').hidden)closeModal();if((e.key==='Enter'||e.key===' ')&&e.target.matches('.auction-card'))openDetail(Number(e.target.dataset.id));});
document.addEventListener('submit',async e=>{
  const form=e.target;if(!['loginForm','registerForm','createForm','editForm','bidForm','autoForm'].includes(form.id))return;
  e.preventDefault();const button=form.querySelector('button[type="submit"],button:not([type])');if(button)button.disabled=true;
  try{
    const data=Object.fromEntries(new FormData(form));
    if(form.id==='loginForm')return await loginSubmit(form);
    if(form.id==='registerForm'){await api('/auth/register','POST',data);notify('Tạo tài khoản thành công! Hãy đăng nhập.');showLogin();return;}
    if(form.id==='createForm'){
      data.startPrice=Number(data.startPrice);data.startTime=new Date(data.startTime).getTime();data.endTime=new Date(data.endTime).getTime();
      const result=await api('/auctions','POST',data);closeModal();await refresh();notify('Đăng sản phẩm thành công!');return openDetail(result.id);
    }
    if(form.id==='editForm'){
      let id=Number(form.dataset.id);await api('/auctions/'+id,'PUT',data);await refresh();notify('Cập nhật thành công');return openDetail(id);
    }
    const id=detailId;
    if(form.id==='bidForm'){data.amount=Number(data.amount);await api('/auctions/'+id+'/bid','POST',data);notify('Đặt giá thành công!');}
    if(form.id==='autoForm'){data.maxAmount=Number(data.maxAmount);data.increment=Number(data.increment);await api('/auctions/'+id+'/auto','POST',data);notify('Đã cấu hình đấu giá tự động!');}
    await refresh(false);return openDetail(id);
  }catch(error){notify(error.message,true);}
  finally{if(button)button.disabled=false;}
});
$('#openLogin').onclick=showLogin;$('#openRegister').onclick=showRegister;
$('#heroCreate').onclick=showCreate;$('#refreshBtn').onclick=()=>refresh();
$('#search').addEventListener('input',e=>{keyword=e.target.value.trim().toLowerCase();renderAuctions();});
$('#category').addEventListener('change',e=>{categoryFilter=e.target.value;renderAuctions();});
$('#statusFilters').addEventListener('click',e=>{const button=e.target.closest('[data-status]');if(!button)return;statusFilter=button.dataset.status;$('#statusFilters').querySelectorAll('.chip').forEach(ch=>ch.classList.toggle('active',ch===button));renderAuctions();});
async function boot(){
  if(token){try{user=await api('/me');}catch{logoutLocal();}}renderUser();await refresh();
  // SSE triggers refresh on a pushed event. No repeated network polling.
  const stream=new EventSource('/api/events');stream.onmessage=()=>{
    const previous=detailId;const version=++liveVersion;
    refresh(false).then(()=>{if(previous&&detailId===previous&&version===liveVersion)openDetail(previous);});
  };
  stream.onerror=()=>{}; // EventSource reconnects automatically.
  setInterval(()=>{
    document.querySelectorAll('.auction-card .card-footer>span:first-child').forEach((el,i)=>{
      // Counters remain local; server synchronizes state via SSE.
      const id=Number(el.closest('.auction-card')?.dataset.id);const a=auctions.find(x=>x.id===id);
      if(a)el.textContent='◷ '+statusText(a);
    });
    if(detailId){const a=auctions.find(x=>x.id===detailId);const label=document.querySelector('.timer-label');if(a&&label)label.textContent=statusText(a);}
  },1000);
}
boot();
