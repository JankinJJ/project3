package com.stylefeng.guns.rest.modular.auth.filter;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.stylefeng.guns.core.base.tips.ErrorTip;
import com.stylefeng.guns.core.util.RenderUtil;
import com.stylefeng.guns.rest.common.exception.BizExceptionEnum;
import com.stylefeng.guns.rest.config.properties.JwtProperties;
import com.stylefeng.guns.rest.modular.auth.util.JwtTokenUtil;
import com.stylefeng.guns.rest.vo.UserVO;
import io.jsonwebtoken.JwtException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 对客户端请求的jwt token验证过滤器
 *
 * @author fengshuonan
 * @Date 2017/8/24 14:04
 */
public class AuthFilter extends OncePerRequestFilter {

    private final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 放行OPTIONS请求
        if (request.getMethod().equals("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        // 只要携带tooken并且该tooken有效就刷新并放行
        final String requestHeader = request.getHeader(jwtProperties.getHeader());
        if (requestHeader != null) {
            String authToken = requestHeader.substring(7);
            Object o = redisTemplate.opsForValue().get(authToken);
            if (o != null) {
                redisTemplate.expire(authToken, 7, TimeUnit.DAYS);
                chain.doFilter(request, response);
                return;
            }
        }

        // 没有携带tooken则判断是否拦截
        boolean doIntercept = false;
        for (String intercept : jwtProperties.getIntercept().split(",")) {
            if (request.getServletPath().contains(intercept)) {
                doIntercept = true;
                break;
            }
        }
        if (doIntercept) {
            request.getRequestDispatcher("/login").forward(request, response);
            return;
        }

        // 不需要拦截则放行
        chain.doFilter(request, response);
    }
}



//        不起作用（貌似是因为前端的router）
//        response.setContentType("text/html;charset=utf-8");
//        response.getWriter().println("<script>alert(\"请先登录~\")</script>");
//        response.setHeader("refresh", "0;url=http://www.baidu.com");
//        重定向也一样不行
//        response.sendRedirect("http://www.baidu.com");






            //验证token是否过期,包含了验证jwt是否正确
//            try {
//                 boolean flag = jwtTokenUtil.isTokenExpired(authToken);
//                if (flag) {
//                    RenderUtil.renderJson(response, new ErrorTip(BizExceptionEnum.TOKEN_EXPIRED.getCode(), BizExceptionEnum.TOKEN_EXPIRED.getMessage()));
//                    return;
//                }
//            } catch (JwtException e) {
//                //有异常就是token解析失败
//                RenderUtil.renderJson(response, new ErrorTip(BizExceptionEnum.TOKEN_ERROR.getCode(), BizExceptionEnum.TOKEN_ERROR.getMessage()));
//                return;
//            }
//        } else {
//            //header没有带Bearer字段
//            RenderUtil.renderJson(response, new ErrorTip(BizExceptionEnum.TOKEN_ERROR.getCode(), BizExceptionEnum.TOKEN_ERROR.getMessage()));
//            return;
//        }

