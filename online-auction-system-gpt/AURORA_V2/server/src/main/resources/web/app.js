'use strict';
/* Multi-page presentation: every screen has its own URL and HTML document. */
const $ = (selector, root=document) => root.querySelector(selector);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
const formatDate = value => new Date(value).toLocaleString('vi-VN');
const dateShort = value => new Intl.DateTimeFormat('vi-VN',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}).format(new Date(value));
const categories = {ELECTRONICS:'CÔNG NGHỆ',ART:'NGHỆ THUẬT',VEHICLE:'XE CỘ'};
const statuses = {OPEN:'SẮP MỞ',RUNNING:'ĐANG DIỄN RA',FINISHED:'ĐÃ KẾT THÚC',PAID:'ĐÃ THANH TOÁN',CANCELED:'ĐÃ HỦY'};
const page = document.body.dataset.page;
let token = localStorage.getItem('aurora_token') || '';
let user = null;
let auctions = [];
let detailData = null;
let eventStream = null;
let statusFilter = 'ALL', keyword = '', categoryFilter = 'ALL';

async function api(path, method='GET', data) {
  const response = await fetch('/api'+path, {
    method,
    headers: {...(data ? {'Content-Type':'application/json'} : {}), ...(token ? {'Authorization':'Bearer '+token} : {})},
    ...(data ? {body:JSON.stringify(data)} : {})
  });
  let result;
  try { result = await response.json(); } catch { throw Error('Máy chủ trả dữ liệu không hợp lệ'); }
  if (!response.ok) throw Error(result.error || 'Yêu cầu thất bại');
  return result;
}
function notify(message, error=false) {
  const target=$('#toasts'); if (!target) return;
  const el=document.createElement('div'); el.className='toast'+(error?' error':'');
  el.textContent=message; target.append(el); setTimeout(()=>el.remove(),4500);
}
function clearSession() {
  token=''; user=null; localStorage.removeItem('aurora_token');
  if(eventStream){eventStream.close();eventStream=null;}
}
function renderNav() {
  const links=$('#navLinks'), area=$('#userArea');
  links.innerHTML=user ? '<a href="/auctions" class="active">Khám phá</a><a href="/" >Giới thiệu</a>' : '<a href="/">Trang chủ</a><a href="/#how">Cách hoạt động</a>';
  if(user){
    const seller=['SELLER','ADMIN'].includes(user.role);
    area.innerHTML=`<span class="profile-chip">${escapeHtml(user.username)} · ${escapeHtml(user.role)}</span>${seller?'<a class="btn btn-primary" href="/sell/new">+ Đăng sản phẩm</a>':''}<button type="button" class="btn btn-soft" id="logoutButton">Đăng xuất</button>`;
    $('#logoutButton').addEventListener('click', async()=>{
      try{await api('/auth/logout','POST');}catch{}clearSession();location.assign('/');
    });
  }else{
    area.innerHTML='<a class="btn btn-primary btn-login-large" href="/login">Đăng nhập →</a><a class="nav-register-link" href="/register">Đăng ký</a>';
  }
}
function countdown(timestamp) {
  let seconds=Math.max(0,Math.ceil((timestamp-Date.now())/1000));
  const days=Math.floor(seconds/86400);seconds%=86400;
  const hours=Math.floor(seconds/3600);seconds%=3600;
  const minutes=Math.floor(seconds/60);seconds%=60;
  return (days?days+' ngày ':'')+[hours,minutes,seconds].map(v=>String(v).padStart(2,'0')).join(':');
}
function statusText(a) {
  return a.status==='RUNNING' ? `Còn ${countdown(a.endTime)}` : a.status==='OPEN' ? `Mở sau ${countdown(a.startTime)}` : statuses[a.status] || a.status;
}
function card(a) {
  return `<a class="auction-card" href="/auctions/${a.id}" aria-label="Xem chi tiết ${escapeHtml(a.title)}"><div class="card-picture"><span class="card-symbol">${escapeHtml(a.icon)}</span><span class="status-badge ${a.status.toLowerCase()}">${statuses[a.status]}</span><span class="card-id">#${String(a.id).padStart(4,'0')}</span></div><div class="card-body"><span class="card-category">${categories[a.category]||'SẢN PHẨM'}</span><h3 class="card-title">${escapeHtml(a.title)}</h3><div class="card-description">${escapeHtml(a.description)}</div><div class="card-price-row"><div><small>GIÁ HIỆN TẠI</small><strong>${money(a.currentPrice)}</strong></div><div class="bid-number"><span>LƯỢT ĐẤU</span><strong>${a.bidCount} ↗</strong></div></div><div class="card-footer"><span class="dynamic-timer">◷ ${escapeHtml(statusText(a))}</span><span class="view">XEM CHI TIẾT ↗</span></div></div></a>`;
}
function renderAuctions() {
  const items=auctions.filter(a=>
    (statusFilter==='ALL'||a.status===statusFilter||(statusFilter==='FINISHED'&&['PAID','CANCELED'].includes(a.status))) &&
    (categoryFilter==='ALL'||a.category===categoryFilter) &&
    `${a.title} ${a.description} ${a.seller}`.toLowerCase().includes(keyword));
  $('#auctionGrid').innerHTML=items.length?items.map(card).join(''):'<div class="empty">Chưa tìm thấy sản phẩm phù hợp. Hãy thử một bộ lọc khác.</div>';
  $('#catalogSummary').textContent=`${items.length} sản phẩm · ${auctions.filter(a=>a.status==='RUNNING').length} phiên đang diễn ra`;
}
async function refreshCatalog(showError=true) {
  try{auctions=await api('/auctions');renderAuctions();}
  catch(error){if(showError) notify(error.message,true);}
}
function configureCatalog() {
  $('#newAuctionLink').hidden=!['SELLER','ADMIN'].includes(user.role);
  $('#refreshBtn').addEventListener('click',()=>refreshCatalog());
  $('#search').addEventListener('input',event=>{keyword=event.target.value.trim().toLowerCase();renderAuctions();});
  $('#category').addEventListener('change',event=>{categoryFilter=event.target.value;renderAuctions();});
  $('#statusFilters').addEventListener('click',event=>{
    const button=event.target.closest('[data-status]');if(!button)return;
    statusFilter=button.dataset.status;
    $('#statusFilters').querySelectorAll('.chip').forEach(el=>el.classList.toggle('active',el===button));
    renderAuctions();
  });
  refreshCatalog();
}
function buildChart(history, startPrice, startTime) {
  const points=[{amount:startPrice,createdAt:startTime},...history];
  const width=520,height=180,pad=30;
  const values=points.map(point=>point.amount), timestamps=points.map(point=>point.createdAt);
  const min=Math.min(...values),max=Math.max(...values),range=Math.max(max-min,1);
  const from=Math.min(...timestamps),to=Math.max(...timestamps),elapsed=Math.max(to-from,1);
  const x=(point,index)=>points.length===1?pad:pad+(point.createdAt-from)/elapsed*(width-2*pad);
  const y=amount=>height-pad-(amount-min)/range*(height-2*pad);
  const path=points.length===1?`M${pad},${y(startPrice)} L${width-pad},${y(startPrice)}`:
    points.map((pt,i)=>`${i?'L':'M'}${x(pt,i)},${y(pt.amount)}`).join(' ');
  const last=points[points.length-1],lastX=points.length===1?width-pad:x(last,points.length-1);
  return `<svg class="bid-chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="Biểu đồ giá theo thời gian"><defs><linearGradient id="priceFade" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="#fb8953" stop-opacity=".36"/><stop offset="100%" stop-color="#fb8953" stop-opacity="0"/></linearGradient></defs><path d="M${pad},${height-pad} H${width-pad}" stroke="#496075"/><path d="${path} L${width-pad},${height-pad} L${pad},${height-pad} Z" fill="url(#priceFade)"/><path d="${path}" fill="none" stroke="#ffa777" stroke-width="3" stroke-linejoin="round"/><circle cx="${lastX}" cy="${y(last.amount)}" r="5" fill="#ffa777"/><text x="${pad}" y="${height-9}" font-size="10" fill="#bcc6d0">${escapeHtml(dateShort(from))}</text><text x="${width-pad}" y="${height-9}" text-anchor="end" font-size="10" fill="#bcc6d0">${escapeHtml(dateShort(to))}</text></svg>`;
}
const detailId=()=>{
  const match=location.pathname.match(/^\/auctions\/(\d+)/);
  return match?Number(match[1]):null;
};
function bidControls(a) {
  if(user.role!=='BIDDER')return '<div class="permission-note">Bạn đang dùng tài khoản Seller/Admin. Chỉ tài khoản Bidder mới được đặt giá.</div>';
  if(a.status!=='RUNNING')return `<div class="permission-note">Phiên hiện ${escapeHtml(statuses[a.status]?.toLowerCase()||'không hoạt động')}, không thể đặt thêm giá.</div>`;
  return `<h3>Đặt giá của bạn</h3><form class="modal-form" id="bidForm"><label>Mức giá đặt (VNĐ)<input name="amount" type="number" min="${a.currentPrice+1}" step="1" value="${a.currentPrice+1000}" required></label><button class="btn btn-primary btn-lg" type="submit">Xác nhận đặt giá ↗</button></form><p class="form-note">Giá phải lớn hơn giá hiện tại. Giao dịch sẽ được server ghi nhận ngay.</p><div class="divider"></div><h3>⚡ Đấu giá tự động</h3><form class="modal-form" id="autoForm"><div class="form-two"><label>Giá tối đa (VNĐ)<input name="maxAmount" type="number" min="${a.currentPrice+1}" step="1" required></label><label>Bước giá (VNĐ)<input name="increment" type="number" min="1000" step="1000" value="10000" required></label></div><button class="btn btn-ghost" type="submit">Bật đấu giá tự động ↗</button></form>`;
}
function managementControls(a) {
  const manager=user.role==='ADMIN'||user.id===a.sellerId;
  if(!manager)return '';
  const canEdit=!a.bidCount&&['OPEN','RUNNING'].includes(a.status);
  const cancel=['OPEN','RUNNING','FINISHED'].includes(a.status) && (!a.bidCount||user.role==='ADMIN');
  const pay=a.status==='FINISHED'&&a.leaderId;
  return `<div class="seller-actions"><h3>Quản lý phiên đấu giá</h3><div class="detail-actions">${canEdit?`<a class="btn btn-ghost" href="/auctions/${a.id}/edit">Sửa sản phẩm →</a><button type="button" class="btn btn-danger" data-manage="delete">Xóa sản phẩm</button>`:''}${cancel?'<button type="button" class="btn btn-danger" data-manage="cancel">Hủy phiên</button>':''}${pay?'<button type="button" class="btn btn-primary" data-manage="pay">Xác nhận thanh toán</button>':''}</div><div id="inlineConfirm" class="confirm-inline" hidden></div></div>`;
}
function renderDetail(a) {
  detailData=a;
  document.title=`${a.title} — AURORA`;
  const history=a.history.slice().reverse().map(h=>`<div class="bid-row"><div><strong>${escapeHtml(h.bidder)}</strong><span>${h.automatic?'⚡ Auto-bid':'Đặt giá trực tiếp'}</span></div><div><strong>${money(h.amount)}</strong><span>${escapeHtml(formatDate(h.createdAt))}</span></div></div>`).join('');
  $('#detailContent').innerHTML=`<section class="detail-hero"><div class="detail-art">${escapeHtml(a.icon)}</div><div class="detail-heading"><div class="eyebrow orange">${categories[a.category]||'SẢN PHẨM'} · MÃ PHIÊN #${a.id}</div><h1>${escapeHtml(a.title)}</h1><p>${escapeHtml(a.description)}</p><div class="detail-price">${money(a.currentPrice)}</div><div class="detail-meta">Người bán: <b>${escapeHtml(a.seller)}</b> · Dẫn đầu: <b>${escapeHtml(a.leader||'Chưa có')}</b><br>Trạng thái: <b>${escapeHtml(statuses[a.status]||a.status)}</b> · ${a.bidCount} lượt đặt giá<br>Kết thúc: <b>${escapeHtml(formatDate(a.endTime))}</b><br><span class="timer-label">◷ ${escapeHtml(statusText(a))}</span></div></div></section>${managementControls(a)}<section class="auction-details-grid"><div class="detail-panel"><div class="eyebrow orange">ĐẤU GIÁ TRỰC TIẾP</div>${bidControls(a)}</div><div class="detail-panel"><div class="eyebrow orange">DIỄN BIẾN GIÁ</div><h3>Biểu đồ giá theo thời gian</h3>${buildChart(a.history,a.startPrice,a.startTime)}<h3>Lịch sử trả giá (${a.history.length})</h3><div class="bid-history">${history||'<p class="muted">Chưa có lượt trả giá nào.</p>'}</div></div></section>`;
  for(const formId of ['bidForm','autoForm']) {
    const form=$('#'+formId);
    if(form)form.addEventListener('submit',event=>submitBid(event,formId));
  }
  document.querySelectorAll('[data-manage]').forEach(button=>button.addEventListener('click',()=>openInlineConfirmation(button.dataset.manage)));
}
async function refreshDetail(showError=true) {
  try{const result=await api('/auctions/'+detailId());renderDetail(result);}
  catch(error){if(showError){$('#detailContent').innerHTML=`<div class="empty">${escapeHtml(error.message)} <a href="/auctions">← Quay lại danh sách</a></div>`;notify(error.message,true);}}
}
async function submitBid(event,type) {
  event.preventDefault();const form=event.currentTarget;
  const button=$('button[type="submit"]',form);button.disabled=true;
  try{
    const data=Object.fromEntries(new FormData(form));
    if(type==='bidForm') {await api(`/auctions/${detailId()}/bid`,'POST',{amount:Number(data.amount)});notify('Đặt giá thành công!');}
    else {await api(`/auctions/${detailId()}/auto`,'POST',{maxAmount:Number(data.maxAmount),increment:Number(data.increment)});notify('Đã kích hoạt đấu giá tự động!');}
    await refreshDetail(false);
  }catch(error){notify(error.message,true);}finally{button.disabled=false;}
}
function openInlineConfirmation(operation) {
  const box=$('#inlineConfirm');if(!box)return;
  const labels={delete:'Xóa vĩnh viễn sản phẩm này?',cancel:'Hủy phiên đấu giá này?',pay:'Xác nhận người thắng đã thanh toán?'};
  box.hidden=false;
  box.innerHTML=`<p>${labels[operation]}</p><div class="detail-actions"><button class="btn btn-danger" type="button" id="confirmAction">Tôi xác nhận</button><button class="btn btn-soft" type="button" id="cancelAction">Giữ nguyên</button></div>`;
  $('#cancelAction').onclick=()=>{box.hidden=true;box.innerHTML='';};
  $('#confirmAction').onclick=async()=>{
    const btn=$('#confirmAction');btn.disabled=true;
    try {
      const path=`/auctions/${detailId()}${operation==='delete'?'':'/'+operation}`;
      await api(path,operation==='delete'?'DELETE':'POST');
      notify('Thao tác thành công');
      if(operation==='delete')location.assign('/auctions');
      else await refreshDetail(false);
    }catch(error){notify(error.message,true);btn.disabled=false;}
  };
}
function setupCreate() {
  if(!['SELLER','ADMIN'].includes(user.role)){location.replace('/auctions');return;}
  const form=$('#createForm');
  const local=timestamp=>{const d=new Date(timestamp);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16);};
  form.elements.startTime.value=local(Date.now()+120000);
  form.elements.endTime.value=local(Date.now()+3600000);
  form.addEventListener('submit',async event=>{
    event.preventDefault();const button=$('button[type="submit"]',form);button.disabled=true;
    try{
      const data=Object.fromEntries(new FormData(form));
      data.startPrice=Number(data.startPrice);
      data.startTime=new Date(data.startTime).getTime();
      data.endTime=new Date(data.endTime).getTime();
      const result=await api('/auctions','POST',data);
      location.assign('/auctions/'+result.id);
    }catch(error){notify(error.message,true);button.disabled=false;}
  });
}
async function setupEdit() {
  const id=detailId();$('#backToDetail').href='/auctions/'+id;
  const form=$('#editForm');
  try{
    const a=await api('/auctions/'+id);
    if(!(['SELLER','ADMIN'].includes(user.role))||(user.id!==a.sellerId&&user.role!=='ADMIN')||a.bidCount||!['OPEN','RUNNING'].includes(a.status)){
      location.replace('/auctions/'+id);return;
    }
    form.elements.title.value=a.title;form.elements.description.value=a.description;
  }catch(error){notify(error.message,true);location.replace('/auctions');return;}
  form.addEventListener('submit',async event=>{
    event.preventDefault();const button=$('button[type="submit"]',form);button.disabled=true;
    try{const data=Object.fromEntries(new FormData(form));await api('/auctions/'+id,'PUT',data);location.assign('/auctions/'+id);}
    catch(error){notify(error.message,true);button.disabled=false;}
  });
}
function configureAuth() {
  if(page==='login') {
    if(new URLSearchParams(location.search).get('registered')==='1'){
      const box=$('#authMessage');box.hidden=false;box.textContent='Đăng ký thành công! Hãy đăng nhập để tiếp tục.';
    }
    $('#loginForm').addEventListener('submit',async event=>{
      event.preventDefault();const form=event.currentTarget,button=$('button',form);button.disabled=true;
      try{
        const result=await api('/auth/login','POST',Object.fromEntries(new FormData(form)));
        token=result.token;user=result.user;localStorage.setItem('aurora_token',token);
        location.replace('/auctions');
      }catch(error){notify(error.message,true);button.disabled=false;}
    });
  } else {
    $('#registerForm').addEventListener('submit',async event=>{
      event.preventDefault();const form=event.currentTarget,button=$('button',form);button.disabled=true;
      const data=Object.fromEntries(new FormData(form));
      if(data.password!==data.confirmPassword){notify('Mật khẩu nhập lại chưa khớp',true);button.disabled=false;return;}
      delete data.confirmPassword;
      try{await api('/auth/register','POST',data);location.assign('/login?registered=1');}
      catch(error){notify(error.message,true);button.disabled=false;}
    });
  }
}
function connectEvents() {
  // Server-Sent Events push updates; no repeated network polling.
  if(!['list','detail'].includes(page))return;
  eventStream=new EventSource('/api/events');
  eventStream.onmessage=()=>{if(page==='list')refreshCatalog(false);else refreshDetail(false);};
}
async function boot() {
  if(token){
    try{user=await api('/me');}
    catch{clearSession();}
  }
  renderNav();
  if(['home','login','register'].includes(page)) {
    if(user){location.replace('/auctions');return;}
    if(page==='login'||page==='register')configureAuth();
    return;
  }
  if(!user){location.replace('/login');return;}
  if(page==='create'&&!['SELLER','ADMIN'].includes(user.role)){location.replace('/auctions');return;}
  $('#protectedMain').hidden=false;
  if(page==='list')configureCatalog();
  if(page==='detail')await refreshDetail();
  if(page==='create')setupCreate();
  if(page==='edit')await setupEdit();
  connectEvents();
  // Countdowns update locally; auction state is pushed by SSE.
  if(page==='list'||page==='detail')setInterval(()=>{
    if(page==='list'){
      for(const el of document.querySelectorAll('.auction-card')){
        const id=Number(el.getAttribute('href').split('/').pop()),a=auctions.find(auction=>auction.id===id);
        if(a){const timer=$('.dynamic-timer',el);if(timer)timer.textContent='◷ '+statusText(a);}
      }
    }else if(detailData){const timer=$('.timer-label');if(timer)timer.textContent='◷ '+statusText(detailData);}
  },1000);
}
boot();
