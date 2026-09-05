package com.cosx.knowengine;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cosx.knowengine.common.enums.UserPermission;
import com.cosx.knowengine.dto.auth.BootstrapAdminRequest;
import com.cosx.knowengine.dto.auth.LoginRequest;
import com.cosx.knowengine.dto.auth.LoginResponse;
import com.cosx.knowengine.dto.permission.PermissionCreateRequest;
import com.cosx.knowengine.dto.permission.PermissionQuery;
import com.cosx.knowengine.dto.permission.PermissionResponse;
import com.cosx.knowengine.dto.user.GrantPermissionsRequest;
import com.cosx.knowengine.dto.user.UserCreateRequest;
import com.cosx.knowengine.dto.user.UserQuery;
import com.cosx.knowengine.dto.user.UserResponse;
import com.cosx.knowengine.security.CurrentUser;
import com.cosx.knowengine.security.UserContextHolder;
import com.cosx.knowengine.service.AuthService;
import com.cosx.knowengine.service.PermissionService;
import com.cosx.knowengine.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthPermissionIntegrationTests.InMemorySaTokenDaoConfig.class)
class AuthPermissionIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpSaTokenContext() {
        SaTokenContextMockUtil.setMockContext();
    }

    @AfterEach
    void clearContexts() {
        UserContextHolder.clear();
        SaTokenContextMockUtil.clearContext();
    }

    @Test
    void shouldLoginGrantPermissionsAndPageUsers() {
        LoginResponse adminLogin = authService.bootstrapAdmin(
                new BootstrapAdminRequest("admin_01", "ChangeMe123!", "管理员"));
        assertThat(adminLogin.tokenValue()).isNotBlank();
        StpUtil.checkPermission("ADMIN");
        StpUtil.checkPermission("NORMAL");

        PermissionResponse normalPermission = permissionService.create(
                new PermissionCreateRequest(UserPermission.NORMAL));
        UserResponse normalUser = userService.create(
                new UserCreateRequest("reader_01", "Reader123!", "普通用户", "reader@example.com"));
        userService.grantPermissions(
                normalUser.id(), new GrantPermissionsRequest(Set.of(normalPermission.id())));

        StpUtil.logout();
        LoginResponse userLogin = authService.login(new LoginRequest("reader_01", "Reader123!"));
        assertThat(userLogin.user().permissions()).containsExactly("NORMAL");
        StpUtil.checkPermission("NORMAL");
        assertThatThrownBy(() -> StpUtil.checkPermission("ADMIN"))
                .isInstanceOf(NotPermissionException.class);

        UserQuery userQuery = new UserQuery();
        userQuery.setPage(1);
        userQuery.setSize(10);
        assertThat(userService.page(userQuery).total()).isEqualTo(2);

        PermissionQuery permissionQuery = new PermissionQuery();
        assertThat(permissionService.page(permissionQuery).total()).isEqualTo(2);

        UserContextHolder.set(new CurrentUser(normalUser.id(), "reader_01", "普通用户", List.of("NORMAL")));
        assertThat(UserContextHolder.requireCurrentUser().username()).isEqualTo("reader_01");
        UserContextHolder.clear();
        assertThat(UserContextHolder.get()).isEmpty();
    }

    @Test
    void shouldEnforceGatewayPermissionsAndClearThreadLocal() throws Exception {
        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isUnauthorized());

        String bootstrapBody = """
                {"username":"admin_02","password":"ChangeMe123!","nickname":"Admin"}
                """;
        String responseBody = mockMvc.perform(post("/v1/auth/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bootstrapBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JSONObject data = JSON.parseObject(responseBody).getJSONObject("data");
        String adminToken = data.getString("tokenValue");

        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(UserContextHolder.get()).isEmpty();

        PermissionResponse normalPermission = permissionService.create(
                new PermissionCreateRequest(UserPermission.NORMAL));
        UserResponse normalUser = userService.create(
                new UserCreateRequest("reader_02", "Reader123!", "Reader", null));
        userService.grantPermissions(
                normalUser.id(), new GrantPermissionsRequest(Set.of(normalPermission.id())));
        String loginBody = """
                {"username":"reader_02","password":"Reader123!"}
                """;
        String loginResponseBody = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String normalToken = JSON.parseObject(loginResponseBody)
                .getJSONObject("data")
                .getString("tokenValue");

        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + normalToken))
                .andExpect(status().isForbidden());
        assertThat(UserContextHolder.get()).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class InMemorySaTokenDaoConfig {

        @Bean
        @Primary
        SaTokenDao testSaTokenDao() {
            return new SaTokenDaoDefaultImpl();
        }
    }
}
