package controller.test1;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

@MyRestController
public class UserController {
    private static List<User> userList = new ArrayList<>();

    public static Map<String, Object> beanMap = new HashMap<>();
    private static Map<String,MethodInfo> methodMap=new HashMap<>();

    static {
        userList.add(new User(1, "Jim"));
        userList.add(new User(2, "jack"));
    }

    @MyRequestMapping("/get")
    public String get(int id) {
        return userList.get(id-1).toString();
    }
    @MyRequestMapping("/add")
    public String addUser(User user){
        userList.add(user);
        return userList.toString();
    }

    @MyRequestMapping("/getAll")
    public String getAll() {
        return userList.toString();
    }

    public static void main(String[] args) throws Exception {

        refreshBeanFactory("controller.test1");
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(80));
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Http Server Start");
        while (true){
            if(selector.select(3000)==0){
                continue;
            }
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();
                httpHandle(key);
                keyIterator.remove();
            }
        }
    }

    private static void refreshBeanFactory(String pkg) {
        String path = pkg.replace(".", "/");
        URL url = UserController.class.getClassLoader().getResource(path);
        File rootDir = new File(url.getPath());
//        System.out.println(url.getPath());
        beanParse(rootDir);
    }

    private static void beanParse(File rootDir) {
        if(!rootDir.isDirectory()){
            return;
        }
        File[] files = rootDir.listFiles(pathname -> {
            if(pathname.isDirectory()){
                beanParse(pathname);
                return false;
            }
            return pathname.getName().endsWith(".class");});
        for (File f:files) {
//            System.out.println(file.getName());
            String filePath = f.getAbsolutePath();
//            System.out.println(filePath);
            String className = filePath.split("classes\\\\")[1].split("\\.class")[0].replace("\\",".");
//            System.out.println(className);
            try {
                Class<?> cls = Class.forName(className);
                MyRestController myRestController = cls.getAnnotation(MyRestController.class);
                if(myRestController!=null){
                    controllerParse(cls);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void controllerParse(Class<?> cls) {
        try {
            beanMap.put(cls.getSimpleName(),cls.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Method[] methods = cls.getDeclaredMethods();
        for (Method method:methods) {
            MyRequestMapping myRequestMapping = method.getDeclaredAnnotation(MyRequestMapping.class);
            if(myRequestMapping==null){
                continue;
            }
            String url = myRequestMapping.value();
//            System.out.println("cls.getName() = " + cls.getName());
            methodMap.put(url,new MethodInfo(cls.getSimpleName(), method));
        }
    }

    private static String methodInvoke(String url, List<String> urlParams) throws InvocationTargetException, IllegalAccessException {
        MethodInfo methodInfo = methodMap.get(url);
        if(methodInfo==null){
            return "404";
        }
        String className = methodInfo.getClassName();
        Method method = methodInfo.getMethod();
        Object beanObj = beanMap.get(className);
        Object[] params = new Object[urlParams.size()];
        Parameter[] parameters = method.getParameters();
        if(parameters.length!=params.length){
            return "参数个数不匹配";
        }
        int i=0;
        for (Parameter p:parameters) {
            String type = p.getType().getSimpleName();
            String pName = p.getName();
//            System.out.println("type = " + type);
//            System.out.println("pName = " + pName);
            boolean flag=false;
            for (String p2:urlParams) {
                String[] pp = p2.split("=");
                if(pName.equals(pp[0].trim())){
                    Object pValue=paramTranslate(type,pp[1]);
                    params[i++]=pValue;
                    flag=true;
                    continue;
                }
            }
            if(!flag){
                return "参数名称不匹配";
            }
        }
        return (String) method.invoke(beanObj,params);
    }

    private static Object paramTranslate(String type, String s) {
//        System.out.println("--------------------------------------------------");
//        System.out.println("s = " + s);
        switch (type){
            case "int":
                return Integer.valueOf(s);
            case "double":
                return Double.valueOf(s);
            case "float":
                return Float.valueOf(s);
            case "user":
                String[] split = s.split(",");
                return new User(Integer.parseInt(split[0]),split[1]);
            default:
                return s;
        }
    }

    private static void urlParamsParse(String url, List<String> urlParams) {
        if (!url.contains("?")) {
            return;
        }
        String[] ps = url.replaceFirst(".*?\\?", "").split("&");
        for(String p:ps){
            if(!p.contains("=")){
                continue;
            }
            urlParams.add(p);
        }

    }
    
    private static void httpHandle(SelectionKey key) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (key.isAcceptable()) {
            acceptHandle(key);
        } else if (key.isReadable()) {
            requestHandle(key);
        }
    }

    private static void requestHandle(SelectionKey key) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        SocketChannel socketChannel= (SocketChannel) key.channel();
        ByteBuffer byteBuffer=(ByteBuffer)key.attachment();
        byteBuffer.clear();
        if(socketChannel.read(byteBuffer)==-1){
            socketChannel.close();
            return;
        }
        byteBuffer.flip();
        String requestMsg = new String(byteBuffer.array());
        String url= requestMsg.split("\r\n")[0].split(" ")[1];
        if(url.equals("/favicon.ico")){
            return;
        }
        System.out.println(requestMsg);
        System.out.println("Request: "+url);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HTTP/1.1 200 OK\r\n");
        stringBuilder.append("Content-Type:text/html;charset=utf-8\r\n");
        stringBuilder.append("\r\n");
        stringBuilder.append("<html><head><title>HttpTest</title></head><body>");

        List<String> urlParams = new ArrayList<>();
        String content;
        urlParamsParse(url,urlParams);
        url = url.split("\\?")[0];
        content=methodInvoke(url,urlParams);
        stringBuilder.append(content!=null?content:"404");
        stringBuilder.append("</body></html>");
        socketChannel.write(ByteBuffer.wrap(stringBuilder.toString().getBytes()));
        socketChannel.close();
    }
    
    private static void acceptHandle(SelectionKey key) throws IOException {
        SocketChannel socketChannel=((ServerSocketChannel)key.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(key.selector(),SelectionKey.OP_READ, ByteBuffer.allocate(1024));
    }
}
