package vn.uet.auction.client;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Client-side Model gateway. Never opens persistence/database files. */
public final class ApiClient {
    private final String base=System.getenv().getOrDefault("AUCTION_URL","http://127.0.0.1:8080");
    private final HttpClient http=HttpClient.newBuilder().build();
    private String token="";
    public void setToken(String value){token=value;}
    public JsonElement send(String method,String path,JsonObject payload){
        try{
            HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(base+"/api"+path))
                .header("Accept","application/json").header("Authorization","Bearer "+token);
            if(payload!=null)builder.header("Content-Type","application/json");
            if(method.equals("GET"))builder.GET();
            else builder.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(payload.toString()));
            HttpResponse<String> response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonElement data=JsonParser.parseString(response.body());
            if(response.statusCode()>=400)throw new IllegalStateException(data.getAsJsonObject().get("error").getAsString());
            return data;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Mất kết nối",e);}
         catch(IOException e){throw new IllegalStateException("Không kết nối được server: "+base,e);}
    }
    /** Persistent SSE stream; UI controller schedules updates on FX application thread. */
    public void subscribe(Consumer<String> onEvent){
        Thread thread=new Thread(()->{
            while(!Thread.currentThread().isInterrupted()){
                HttpURLConnection connection=null;
                try{
                    connection=(HttpURLConnection)new URL(base+"/api/events").openConnection();
                    connection.setRequestProperty("Accept","text/event-stream");
                    connection.setConnectTimeout(5000);connection.setReadTimeout(0);
                    try(BufferedReader in=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8))){
                        String line;while((line=in.readLine())!=null){if(line.startsWith("data: "))onEvent.accept(line.substring(6));}
                    }
                }catch(IOException ignored){ }
                finally{if(connection!=null)connection.disconnect();}
                try{Thread.sleep(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            }
        },"auction-sse-listener");thread.setDaemon(true);thread.start();
    }
    public static JsonObject json(Object... values){
        JsonObject object=new JsonObject();
        for(int i=0;i<values.length;i+=2){
            String key=(String)values[i];Object value=values[i+1];
            if(value instanceof Number n)object.addProperty(key,n);
            else if(value instanceof Boolean b)object.addProperty(key,b);
            else object.addProperty(key,String.valueOf(value));
        }
        return object;
    }
}
