package com.amarkatha.identity;

import com.amarkatha.identity.domain.User;

public record OAuthLoginResult(User user, boolean newAccount) {
}
