package com.ai.kb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try{
            //从请求中提取JWTTOken
            String jwt = getJwtFromRequest(request);

            //验证Token是否有效
            if(StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)){
                //从令牌提取userid
                String userId = tokenProvider.getUserIdFromToken(jwt);

                //构建Spring Security的认证对象
                //暂时传空的角色列表
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //将认证信息存入上下文，这样Controller知道谁在操作
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }catch (Exception ex){
            //记录日志
            logger.error("Failed", ex);
        }

        //继续执行后面的过滤器（important）
        filterChain.doFilter(request, response);
    }

    //从Authorization请求头提取token
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        //约定格式Bearer <token>
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

