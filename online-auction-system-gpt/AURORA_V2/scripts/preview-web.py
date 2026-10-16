#!/usr/bin/env python3
"""Optional UI-only screenshot preview using mocked API responses (not production E2E)."""
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parent.parent
WEB=ROOT/'server/src/main/resources/web'
html=(WEB/'index.html').read_text().replace('<link rel="stylesheet" href="/style.css" />','').replace('<script src="/app.js" defer></script>','')
css=(WEB/'style.css').read_text()
js=(WEB/'app.js').read_text()
fixtures=[
 {'id':4,'title':'Máy ảnh Fujifilm X100V','description':'Thiết kế cổ điển, cảm biến APS-C, màu ảnh tuyệt đẹp.','category':'ELECTRONICS','icon':'💻','startPrice':22000000,'currentPrice':22500000,'startTime':1,'endTime':2090000000000,'status':'RUNNING','version':1,'sellerId':1,'seller':'seller_demo','leaderId':2,'leader':'bidder_demo','bidCount':6},
 {'id':3,'title':'Yamaha Exciter 155 VVA','description':'Xe đời mới, bảo dưỡng định kỳ, đầy đủ giấy tờ.','category':'VEHICLE','icon':'🚗','startPrice':33000000,'currentPrice':33000000,'startTime':1,'endTime':2090000000000,'status':'RUNNING','version':0,'sellerId':1,'seller':'seller_demo','leaderId':None,'leader':None,'bidCount':0},
 {'id':2,'title':'Tranh sơn dầu Hoàng hôn','description':'Tranh sơn dầu vẽ tay, kích thước 60 × 80 cm.','category':'ART','icon':'🎨','startPrice':2500000,'currentPrice':2700000,'startTime':1,'endTime':2090000000000,'status':'RUNNING','version':0,'sellerId':1,'seller':'seller_demo','leaderId':2,'leader':'bidder_demo','bidCount':3},
 {'id':1,'title':'MacBook Pro M3 Pro 14 inch','description':'Laptop đồ họa cao cấp, RAM 18GB, SSD 512GB.','category':'ELECTRONICS','icon':'💻','startPrice':32000000,'currentPrice':33000000,'startTime':1,'endTime':2090000000000,'status':'RUNNING','version':1,'sellerId':1,'seller':'seller_demo','leaderId':2,'leader':'bidder_demo','bidCount':8},
]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
 page=browser.new_page(viewport={'width':1440,'height':980},device_scale_factor=1)
 errors=[];page.on('pageerror',lambda exc:errors.append(str(exc)))
 page.goto('about:blank')
 page.set_content(html,wait_until='domcontentloaded')
 page.add_style_tag(content=css)
 page.evaluate("""(items)=>{
   const stub={getItem(){return null;},setItem(){},removeItem(){}};
   Object.defineProperty(window,'localStorage',{configurable:true,value:stub});
   window.fetch=async(uri,options)=>({ok:true,status:200,json:async()=>uri.includes('/auctions')?items:{ok:true}});
   window.EventSource=class{constructor(path){this.path=path;}close(){}};
 }""",fixtures)
 page.add_script_tag(content=js)
 page.wait_for_timeout(1200)
 print('Cards rendered:',page.locator('.auction-card').count(),'Errors:',errors)
 page.screenshot(path=str(ROOT/'docs'/'UI_PREVIEW.png'),full_page=True)
 page.get_by_role('button',name='Đăng nhập').first.click()
 print('Login visible:',page.locator('#loginForm').count(),'Errors:',errors)
 browser.close()
