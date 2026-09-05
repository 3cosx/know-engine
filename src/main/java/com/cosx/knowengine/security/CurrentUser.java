package com.cosx.knowengine.security;

import java.util.List;

public record CurrentUser(Long id, String username, String nickname, List<String> permissions) {
}
