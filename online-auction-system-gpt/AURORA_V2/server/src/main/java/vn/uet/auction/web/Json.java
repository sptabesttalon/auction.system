package vn.uet.auction.web;

import java.util.*;

/** Small dependency-free JSON implementation for the project protocol. */
public final class Json {
    private Json() { }
    public static String stringify(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String str) return '"' + str.replace("\\","\\\\").replace("\"","\\\"")
            .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t").replace("\b","\\b").replace("\f","\\f") + '"';
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?,?> map) {
            StringJoiner join=new StringJoiner(",","{","}");
            map.forEach((k,v)->join.add(stringify(String.valueOf(k))+":"+stringify(v)));
            return join.toString();
        }
        if (obj instanceof Iterable<?> list) {
            StringJoiner join=new StringJoiner(",","[","]");
            list.forEach(v->join.add(stringify(v)));return join.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON: "+obj.getClass());
    }
    public static Object parse(String json) {
        Parser p=new Parser(json);
        Object value=p.readValue();p.space();
        if (p.pos!=json.length()) throw new IllegalArgumentException("JSON dư dữ liệu");
        return value;
    }
    private static final class Parser {
        final String s;int pos;
        Parser(String s){this.s=s;}
        void space(){while(pos<s.length()&&Character.isWhitespace(s.charAt(pos)))pos++;}
        char peek(){space();if(pos>=s.length())throw new IllegalArgumentException("JSON không đầy đủ");return s.charAt(pos);}
        void expect(char ch){if(peek()!=ch)throw new IllegalArgumentException("JSON lỗi ký tự "+pos);pos++;}
        Object readValue(){
            char c=peek();
            if(c=='"')return str();
            if(c=='{')return object();
            if(c=='[')return array();
            if(c=='t'&&s.startsWith("true",pos)){pos+=4;return true;}
            if(c=='f'&&s.startsWith("false",pos)){pos+=5;return false;}
            if(c=='n'&&s.startsWith("null",pos)){pos+=4;return null;}
            if(c=='-'||Character.isDigit(c))return number();
            throw new IllegalArgumentException("JSON lỗi giá trị tại "+pos);
        }
        String str(){
            expect('"');StringBuilder out=new StringBuilder();
            while(pos<s.length()){
                char c=s.charAt(pos++);if(c=='"')return out.toString();
                if(c=='\\'){
                    if(pos>=s.length())break;
                    c=s.charAt(pos++);
                    c=switch(c){case 'n'->'\n';case 't'->'\t';case 'r'->'\r';case 'b'->'\b';case 'f'->'\f';case '/'->'/';case '"'->'"';case '\\'->'\\';
                        case 'u'->{if(pos+4>s.length())throw new IllegalArgumentException("Unicode không đầy đủ");char code=(char)Integer.parseInt(s.substring(pos,pos+4),16);pos+=4;yield code;}
                        default->throw new IllegalArgumentException("Escape JSON không hợp lệ");};
                }else if(c<32)throw new IllegalArgumentException("JSON chứa ký tự điều khiển");
                out.append(c);
            }
            throw new IllegalArgumentException("JSON thiếu dấu nháy");
        }
        Map<String,Object> object(){
            expect('{');Map<String,Object> r=new LinkedHashMap<>();
            if(peek()=='}'){pos++;return r;}
            while(true){String key=str();expect(':');r.put(key,readValue());
                char c=peek();if(c=='}'){pos++;return r;}expect(',');}
        }
        List<Object> array(){
            expect('[');List<Object> r=new ArrayList<>();
            if(peek()==']'){pos++;return r;}
            while(true){r.add(readValue());char c=peek();if(c==']'){pos++;return r;}expect(',');}
        }
        Number number(){
            int start=pos;if(s.charAt(pos)=='-')pos++;
            while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;
            boolean floating=false;
            if(pos<s.length()&&s.charAt(pos)=='.'){floating=true;pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}
            if(pos<s.length()&&(s.charAt(pos)=='e'||s.charAt(pos)=='E')){floating=true;pos++;if(pos<s.length()&&(s.charAt(pos)=='+'||s.charAt(pos)=='-'))pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}
            String n=s.substring(start,pos);
            try{if(floating)return Double.valueOf(n);return Long.valueOf(n);}catch(NumberFormatException e){throw new IllegalArgumentException("Giá trị số JSON không hợp lệ");}
        }
    }
}
