package com.yao.crm.security;

import java.util.Map;

public interface OidcTokenVerifier {
    Map<String, Object> verify(String idToken, String jwksUri);
}
