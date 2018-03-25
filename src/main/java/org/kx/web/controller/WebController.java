package org.kx.web.controller;

import org.kx.web.annotation.Dispather;
import org.kx.web.annotation.RequesPram;
import org.kx.web.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * create by sunkx on 2018/3/25
 */
@Dispather
@RequestMapping("nick")
public class WebController {

    @RequestMapping("/test1")
    public void test1(HttpServletRequest request, HttpServletResponse response,  @RequesPram("param") String param){
        System.out.println(param);
        try {
            response.getWriter().write( "test1 method success! param:"+param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
