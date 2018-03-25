package org.kx.web.servlet;

import org.kx.web.annotation.Dispather;
import org.kx.web.annotation.RequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * create by sunkx on 2018/3/25
 */
public class DispatcherServlet extends HttpServlet{



    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> beans = new HashMap<>();

    private Map<String, Method> urlMethodMapping = new  HashMap<>();

    private Map<String, Object> urlBeanMapping  =new HashMap<>();


    @Override
    public void init(ServletConfig config) throws ServletException {



        //2.扫描controller 下的class
        doScanner("org.kx.web.controller");

        //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        makeInstance();

        //4.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();

    }






    private void doScanner(String packageName) {
        //把所有的.替换成/
        URL url  =this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if(file.isDirectory()){
                //递归读取包
                doScanner(packageName+"."+file.getName());
            }else{
                String className =packageName +"." +file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }


    private void makeInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                //把类搞出来,反射来实例化(只有加@MyController需要实例化)
                Class<?> clazz =Class.forName(className);
                if(clazz.isAnnotationPresent(Dispather.class)){
                    System.out.println("------");
                    beans.put(toLowerFirstWord(clazz.getSimpleName()),clazz.newInstance());
                }else{
                    System.out.println("=======");
                    continue;
                }

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }


    private void initHandlerMapping(){
        if(beans.isEmpty()){
            return;
        }
        try {
            for (Map.Entry<String, Object> entry: beans.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                if(!clazz.isAnnotationPresent(Dispather.class)){
                    continue;
                }

                //拼url时,是controller头的url拼上方法上的url
                String baseUrl ="";
                if(clazz.isAnnotationPresent(RequestMapping.class)){
                    RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                    baseUrl=annotation.value();
                    if(!baseUrl.startsWith("/")){
                        baseUrl = "/"+baseUrl;
                    }

                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if(!method.isAnnotationPresent(RequestMapping.class)){
                        continue;
                    }
                    RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                    String url = annotation.value();

                    url =(baseUrl+"/"+url).replaceAll("/+", "/");
                    urlMethodMapping.put(url,method);

                    urlBeanMapping.put(url,entry.getValue());

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    /**
     * 把字符串的首字母小写
     */
    private String toLowerFirstWord(String name){
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doDispatch(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doDispatch(req,resp);
    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {

        try {

            if (urlMethodMapping.isEmpty()) {
                return;
            }

            String url = req.getRequestURI();
            String contextPath = req.getContextPath();

            url = url.replace(contextPath, "").replaceAll("/+", "/");


            if (!this.urlMethodMapping.containsKey(url)) {
                resp.getWriter().write("404 NOT FOUND!");
                return;
            }

            Method method = this.urlMethodMapping.get(url);

            //获取方法的参数列表
            Class<?>[] parameterTypes = method.getParameterTypes();

            //获取请求的参数
            Map<String, String[]> parameterMap = req.getParameterMap();

            //保存参数值
            Object[] paramValues = new Object[parameterTypes.length];

            //方法的参数列表
            for (int i = 0; i < parameterTypes.length; i++) {
                //根据参数名称，做某些处理
                String requestParam = parameterTypes[i].getSimpleName();


                if (requestParam.equals("HttpServletRequest")) {
                    //参数类型已明确，这边强转类型   ，将HttpServletRequest作为参数
                    paramValues[i] = req;
                    continue;
                }
                if (requestParam.equals("HttpServletResponse")) {
                    //HttpServletResponse作为参数
                    paramValues[i] = resp;
                    continue;
                }
                if (requestParam.equals("String")) {
                    for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                        String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                        paramValues[i] = value;
                    }
                }
            }
            //利用反射机制来调用
            try {
                method.invoke(this.urlBeanMapping.get(url), paramValues);//第一个参数是method所对应的实例 在ioc容器中
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
