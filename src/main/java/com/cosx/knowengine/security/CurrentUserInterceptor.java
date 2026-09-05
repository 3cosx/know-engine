package com.cosx.knowengine.security;

import cn.dev33.satoken.stp.StpUtil;
import com.cosx.knowengine.entity.UserInfo;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.mapper.SystemPermissionMapper;
import com.cosx.knowengine.mapper.UserInfoMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class CurrentUserInterceptor implements HandlerInterceptor {

    private final UserInfoMapper userMapper;
    private final SystemPermissionMapper permissionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContextHolder.clear();
        if (!StpUtil.isLogin()) {
            return true;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        UserInfo user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            StpUtil.logout();
            throw BusinessException.unauthorized("用户不存在或已被禁用");
        }
        UserContextHolder.set(new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                permissionMapper.selectCodesByUserId(userId)
        ));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        UserContextHolder.clear();
    }
}
