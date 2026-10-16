package vn.uet.auction.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Observer fan-out into Server-Sent Events (push, not polling). */
public final class Events {
    private final CopyOnWriteArrayList<ArrayBlockingQueue<String>> listeners=new CopyOnWriteArrayList<>();
    public void broadcast(String message){
        for(ArrayBlockingQueue<String> q:listeners){if(!q.offer(message)){q.poll();q.offer(message);}}
    }
    public void connect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type","text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control","no-cache, no-transform");
        exchange.getResponseHeaders().set("X-Accel-Buffering","no");
        exchange.sendResponseHeaders(200,0);
        ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<>(128);listeners.add(queue);
        try(OutputStream out=exchange.getResponseBody()){
            out.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));out.flush();
            while(!Thread.currentThread().isInterrupted()){
                String next=queue.poll(15,TimeUnit.SECONDS);
                String frame=next==null?": heartbeat\n\n":"data: "+next+"\n\n";
                out.write(frame.getBytes(StandardCharsets.UTF_8));out.flush();
            }
        }catch(IOException | InterruptedException ignored){Thread.currentThread().interrupt();}
        finally{listeners.remove(queue);exchange.close();}
    }
}
