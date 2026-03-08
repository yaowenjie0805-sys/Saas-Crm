package com.yao.crm.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class OidcRsaJwkTokenVerifier implements OidcTokenVerifier {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public OidcRsaJwkTokenVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> verify(String idToken, String jwksUri) {
        try {
            if (idToken == null || jwksUri == null) {
                return Collections.emptyMap();
            }
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                return Collections.emptyMap();
            }

            Map<String, Object> header = parseJson(decode(parts[0]));
            Map<String, Object> claims = parseJson(decode(parts[1]));
            String kid = string(header.get("kid"));
            String alg = string(header.get("alg"));
            if (!"RS256".equals(alg)) {
                return Collections.emptyMap();
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(jwksUri, Map.class);
            Map body = response.getBody();
            if (body == null) return Collections.emptyMap();
            Object keysObj = body.get("keys");
            if (!(keysObj instanceof List)) return Collections.emptyMap();

            RSAPublicKey key = null;
            for (Object item : (List) keysObj) {
                if (!(item instanceof Map)) continue;
                Map jwk = (Map) item;
                if (!"RSA".equals(string(jwk.get("kty")))) continue;
                String keyId = string(jwk.get("kid"));
                if (kid != null && keyId != null && !kid.equals(keyId)) continue;
                String n = string(jwk.get("n"));
                String e = string(jwk.get("e"));
                if (n == null || e == null) continue;
                key = toRsaPublicKey(n, e);
                break;
            }
            if (key == null) return Collections.emptyMap();

            String signingInput = parts[0] + "." + parts[1];
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(signature)) {
                return Collections.emptyMap();
            }
            return claims;
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private RSAPublicKey toRsaPublicKey(String n, String e) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey key = factory.generatePublic(spec);
        return (RSAPublicKey) key;
    }

    private Map<String, Object> parseJson(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>(){});
    }

    private String decode(String b64) {
        return new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
