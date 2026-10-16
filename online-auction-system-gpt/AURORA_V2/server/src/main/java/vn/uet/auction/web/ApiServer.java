package vn.uet.auction.web;

import com.sun.net.httpserver.*;
import vn.uet.auction.service.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** HTTP Controller: accepts JSON, calls business service, maps exceptions to status codes. */
public final class ApiServer implements AutoCloseable {
    private final AuctionService service;
    private final HttpServer server;
    private final Events events=new Events();
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
    public ApiServer(AuctionService service,String host,int port) throws IOException {
        this.service=service;
        server=HttpServer.create(new InetSocketAddress(host,port),100);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/api/",this::handle);
        server.createContext("/",this::staticFile);
        service.observe(events::broadcast);
    }
    public int port(){return server.getAddress().getPort();}
    public void start(){
        server.start();scheduler.scheduleAtFixedRate(()->{
            try{service.tick();}catch(Exception e){System.err.println("Scheduler: "+e.getMessage());}
        },0,500,TimeUnit.MILLISECONDS);
    }
    @Override public void close(){scheduler.shutdownNow();server.stop(0);}
    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path=exchange.getRequestURI().getPath();String method=exchange.getRequestMethod();
            if(path.equals("/api/events") && method.equals("GET")){events.connect(exchange);return;}
            Object result=route(path,method,exchange);
            send(exchange,200,result);
        }catch(ServiceException e){send(exchange,e.statusCode,Map.of("error",e.getMessage()));}
        catch(IllegalArgumentException e){send(exchange,400,Map.of("error",e.getMessage()==null?"Dữ liệu sai":e.getMessage()));}
        catch(Exception e){e.printStackTrace();send(exchange,500,Map.of("error","Lỗi máy chủ, vui lòng thử lại"));}
    }
    private Object route(String path,String method,HttpExchange ex)throws IOException {
        if(method.equals("GET")&&path.equals("/api/health"))return Map.of("ok",true,"serverTime",System.currentTimeMillis());
        if(method.equals("POST")&&path.equals("/api/auth/register")){
            Map<String,Object> m=body(ex);return service.register(str(m,"username"),str(m,"password"),str(m,"role"));
        }
        if(method.equals("POST")&&path.equals("/api/auth/login")){
            Map<String,Object> m=body(ex);return service.login(str(m,"username"),str(m,"password"));
        }
        if(method.equals("POST")&&path.equals("/api/auth/logout")){service.logout(token(ex));return Map.of("ok",true);}
        if(method.equals("GET")&&path.equals("/api/me"))return service.me(token(ex));
        if(method.equals("GET")&&path.equals("/api/auctions"))return service.list();
        if(method.equals("POST")&&path.equals("/api/auctions")){
            Map<String,Object> m=body(ex);
            return service.create(token(ex),str(m,"category"),str(m,"title"),str(m,"description"),num(m,"startPrice"),num(m,"startTime"),num(m,"endTime"));
        }
        if(path.startsWith("/api/auctions/")){
            String[] parts=path.split("/");
            if(parts.length<4||parts.length>5)throw notFound();
            long id=Long.parseLong(parts[3]);
            if(parts.length==4){
                if(method.equals("GET"))return service.detail(id);
                if(method.equals("PUT")){Map<String,Object> m=body(ex);return service.edit(token(ex),id,str(m,"title"),str(m,"description"));}
                if(method.equals("DELETE"))return service.delete(token(ex),id);
            }else if(method.equals("POST")){
                return switch(parts[4]){
                    case "bid"->{Map<String,Object> m=body(ex);yield service.bid(token(ex),id,num(m,"amount"));}
                    case "auto"->{Map<String,Object> m=body(ex);yield service.setAuto(token(ex),id,num(m,"maxAmount"),num(m,"increment"));}
                    case "cancel"->service.cancel(token(ex),id);
                    case "pay"->service.pay(token(ex),id);
                    default->throw notFound();
                };
            }
        }
        throw notFound();
    }
    private static ServiceException notFound(){return new ServiceException(404,"Đường dẫn không tồn tại");}
    private static String token(HttpExchange ex){String a=ex.getRequestHeaders().getFirst("Authorization");return a!=null&&a.startsWith("Bearer ")?a.substring(7):"";}
    private static String str(Map<String,Object> m,String k){Object o=m.get(k);return o==null?null:String.valueOf(o);}
    private static long num(Map<String,Object> m,String k){Object o=m.get(k);if(!(o instanceof Number n)||o instanceof Double)throw new IllegalArgumentException(k+" cần là số nguyên");return n.longValue();}
    private static Map<String,Object> body(HttpExchange ex)throws IOException {
        byte[] bytes=ex.getRequestBody().readNBytes(65537);
        if(bytes.length>65536)throw new IllegalArgumentException("JSON tối đa 64 KB");
        Object value=Json.parse(new String(bytes,StandardCharsets.UTF_8));
        if(!(value instanceof Map<?,?> map))throw new IllegalArgumentException("JSON phải là object");
        @SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)map;
        return result;
    }
    private static void send(HttpExchange ex,int code,Object obj)throws IOException {
        byte[] data=Json.stringify(obj).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control","no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        ex.sendResponseHeaders(code,data.length);
        try(OutputStream out=ex.getResponseBody()){out.write(data);}
    }
    private void staticFile(HttpExchange ex)throws IOException {
        String path=ex.getRequestURI().getPath();
        // Explicit page routes: a real URL per screen, not a modal or hash navigation.
        String resource=switch(path){
            case "/", "/index.html" -> "web/index.html";
            case "/login", "/login.html" -> "web/login.html";
            case "/register", "/register.html" -> "web/register.html";
            case "/auctions", "/auctions.html" -> "web/auctions.html";
            case "/sell/new" -> "web/create.html";
            case "/app.js" -> "web/app.js";
            case "/style.css" -> "web/style.css";
            default -> {
                if (path.matches("/auctions/[1-9][0-9]*")) yield "web/detail.html";
                if (path.matches("/auctions/[1-9][0-9]*/edit")) yield "web/edit.html";
                yield null;
            }
        };
        if(resource==null){ex.sendResponseHeaders(404,-1);ex.close();return;}
        byte[] bytes;
        try(InputStream stream=getClass().getClassLoader().getResourceAsStream(resource)){
            if(stream==null){ex.sendResponseHeaders(404,-1);ex.close();return;}
            bytes=stream.readAllBytes();
        }
        ex.getResponseHeaders().set("Content-Type",resource.endsWith(".css")?"text/css; charset=utf-8":resource.endsWith(".js")?"application/javascript; charset=utf-8":"text/html; charset=utf-8");
        ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'");
        ex.sendResponseHeaders(200,bytes.length);
        try(OutputStream out=ex.getResponseBody()){out.write(bytes);}
    }
}
