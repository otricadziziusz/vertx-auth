/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.auth.test.jwt;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.authentication.CredentialValidationException;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.test.core.VertxTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertNotEquals;

public class JWTAuthProviderTest extends VertxTestBase {

  private JWTAuth authProvider;

  // {"sub":"Paulo","exp":1747055313,"iat":1431695313,"permissions":["read","write","execute"],"roles":["admin","developer","user"]}
  private static final String JWT_VALID = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJQYXVsbyIsImV4cCI6MTc0NzA1NTMxMywiaWF0IjoxNDMxNjk1MzEzLCJwZXJtaXNzaW9ucyI6WyJyZWFkIiwid3JpdGUiLCJleGVjdXRlIl0sInJvbGVzIjpbImFkbWluIiwiZGV2ZWxvcGVyIiwidXNlciJdfQ.UdA6oYDn9s_k7uogFFg8jvKmq9RgITBnlq4xV6JGsCY";

  // {"sub":"Paulo","iat":1400159434,"exp":1400245834,"roles":["admin","developer","user"],"permissions":["read","write","execute"]}
  private static final String JWT_INVALID = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJQYXVsbyIsImlhdCI6MTQwMDE1OTQzNCwiZXhwIjoxNDAwMjQ1ODM0LCJyb2xlcyI6WyJhZG1pbiIsImRldmVsb3BlciIsInVzZXIiXSwicGVybWlzc2lvbnMiOlsicmVhZCIsIndyaXRlIiwiZXhlY3V0ZSJdfQ==.NhHul0OFlmUaatFwNeGBbshVNzac2z_3twEEg57x80s=";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    authProvider = JWTAuth.create(vertx, getConfig());
  }

  private JWTAuthOptions getConfig() {
    return new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("keystore.jceks")
        .setType("jceks")
        .setPassword("secret"));
  }

  @Test
  public void testValidJWT() {
    TokenCredentials authInfo = new TokenCredentials(JWT_VALID);
    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      // assert that the content of the principal is not empty
      assertNotNull(res.principal().getString("sub"));
      assertNotNull(res.principal().getValue("permissions"));
      assertNotNull(res.principal().getValue("roles"));

      testComplete();
    }));
    await();
  }

  @Test
  public void testInValidCredentials() {
    authProvider.authenticate(new JsonObject().put("username", "username").put("password", "password"), onFailure(err -> {
      assertNotNull(err);
      Assert.assertTrue(err instanceof CredentialValidationException);
      testComplete();
    }));
    await();
  }

  @Test
  public void testInvalidJWT() {
    TokenCredentials authInfo = new TokenCredentials(JWT_INVALID);
    authProvider.authenticate(authInfo, onFailure(thr -> {
      assertNotNull(thr);
      testComplete();
    }));
    await();
  }

  @Test
  public void testJWTValidPermission() {
    TokenCredentials authInfo = new TokenCredentials(JWT_VALID);
    authProvider.authenticate(authInfo, onSuccess(user -> {
      assertNotNull(user);
      JWTAuthorization.create("permissions").getAuthorizations(user, res -> {
        assertTrue(res.succeeded());
        assertTrue(PermissionBasedAuthorization.create("write").match(user));
        testComplete();
      });
    }));
    await();
  }

  @Test
  public void testJWTInvalidPermission() {
    TokenCredentials authInfo = new TokenCredentials(JWT_VALID);
    authProvider.authenticate(authInfo, onSuccess(user -> {
      assertNotNull(user);
      JWTAuthorization.create("permissions").getAuthorizations(user, res -> {
        assertTrue(res.succeeded());
        assertFalse(PermissionBasedAuthorization.create("drop").match(user));
        testComplete();
      });
    }));
    await();
  }

  @Test
  public void testGenerateNewToken() {

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo")
      .put("exp", 1747055313)
      .put("iat", 1431695313)
      .put("permissions", new JsonArray()
        .add("read")
        .add("write")
        .add("execute"))
      .put("roles", new JsonArray()
        .add("admin")
        .add("developer")
        .add("user"));

    String token = authProvider.generateToken(payload, new JWTOptions().setSubject("Paulo"));
    assertNotNull(token);
    assertEquals(JWT_VALID, token);
  }

  @Test
  public void testGenerateNewTokenImmutableClaims() {

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    String token0 = authProvider.generateToken(payload, new JWTOptions().addPermission("user"));
    String token1 = authProvider.generateToken(payload, new JWTOptions().addPermission("admin"));

    assertNotEquals(token0, token1);
  }

  @Test
  public void testTokenWithoutTimestamp() {
    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().setExpiresInMinutes(5).setNoTimestamp(true));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      assertTrue(res.attributes().getJsonObject("accessToken").containsKey("exp"));
      assertFalse(res.attributes().getJsonObject("accessToken").containsKey("iat"));
      testComplete();
    }));

    await();
  }

  @Test
  public void testTokenWithTimestamp() {
    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload, new JWTOptions());
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);
    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      assertTrue(res.attributes().getJsonObject("accessToken").containsKey("iat"));
      testComplete();
    }));
    await();
  }

  @Test
  public void testExpiration() {
    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().setExpiresInSeconds(1).setNoTimestamp(true));

    assertNotNull(token);

    vertx.setTimer(2000L, t -> {
      TokenCredentials authInfo = new TokenCredentials(token);
      authProvider.authenticate(authInfo, onFailure(thr -> {
        assertNotNull(thr);
        testComplete();
      }));
    });

    await();
  }

  @Test
  public void testGoodIssuer() {
    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload, new JWTOptions().setIssuer("https://vertx.io"));
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testBadIssuer() {

    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(new JWTOptions().setIssuer("https://vertx.io")));

    JsonObject payload = new JsonObject().put("sub", "Paulo");

    final String token = authProvider.generateToken(payload, new JWTOptions().setIssuer("https://auth0.io"));
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onFailure(thr -> {
      assertNotNull(thr);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGoodAudience() {

    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addAudience("b")
        .addAudience("d")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addAudience("a").addAudience("b").addAudience("c"));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testBadAudience() {

    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addAudience("e")
        .addAudience("d")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addAudience("a").addAudience("b").addAudience("c"));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onFailure(thr -> {
      assertNotNull(thr);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGoodScopes() {
    //JWT is valid because required scopes "a" & "b" are well included in the access_token.
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addScope("a")
        .addScope("b")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addScope("a").addScope("b").addScope("c"));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGoodScopesWithDelimiter() {
    //JWT is valid because required scopes "a" & "b" are well included in the access_token.
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addScope("a")
        .addScope("b")
        .withScopeDelimiter(",")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addScope("a").addScope("b").addScope("c").withScopeDelimiter(","));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGoodScopesWithDefaultDelimiter() {
    //JWT is valid because required scopes "a" & "b" are well included in the access_token.
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addScope("a")
        .addScope("b")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addScope("a").addScope("b").addScope("c").withScopeDelimiter(" "));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testBadScopes() {
    //JWT is not valid because the required scopes "d" is not included in the access_token.
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addScope("b")
        .addScope("d")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addScope("a").addScope("b").addScope("c"));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onFailure(thr -> {
      assertNotNull(thr);
      testComplete();
    }));
    await();
  }

  @Test
  public void testBadScopesFormat() {
    //JWT is not valid because the authProvider is expecting an array of scope while the JWT has a string scope.
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(
      new JWTOptions()
        .addScope("a")
        .addScope("b")));

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    final String token = authProvider.generateToken(payload,
      new JWTOptions().addScope("a").addScope("b").addScope("c").withScopeDelimiter(","));

    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onFailure(thr -> {
      assertNotNull(thr);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGenerateNewTokenES256() {
    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("es256-keystore.jceks")
        .setType("jceks")
        .setPassword("secret")));

    String token = authProvider.generateToken(new JsonObject().put("sub", "paulo"), new JWTOptions().setAlgorithm("ES256"));
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, res -> {
      if (res.failed()) {
        res.cause().printStackTrace();
        fail();
      }

      assertNotNull(res.result());
      testComplete();
    });
    await();
  }

  @Test
  public void testGenerateNewTokenWithMacSecret() {
    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addJwk(new JsonObject()
        .put("kty", "oct")
        .put("k", "notasecret"))
    );

    String token = authProvider.generateToken(new JsonObject(), new JWTOptions().setAlgorithm("HS256"));
    assertNotNull(token);

    // reverse
    TokenCredentials authInfo = new TokenCredentials(token);
    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testValidateTokenWithInvalidMacSecret() {
    String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDE3ODUyMDZ9.08K_rROcCmKTF1cKfPCli2GQFYIOP8dePxeS1SE4dc8";
    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addJwk(new JsonObject()
        .put("kty", "oct")
        .put("k", Base64.getUrlEncoder().encodeToString("a bad secret".getBytes(StandardCharsets.UTF_8))))
    );
    TokenCredentials authInfo = new TokenCredentials(token);
    authProvider.authenticate(authInfo, onFailure(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testValidateTokenWithValidMacSecret() {
    String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDE3ODUyMDZ9.08K_rROcCmKTF1cKfPCli2GQFYIOP8dePxeS1SE4dc8";
    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addJwk(new JsonObject()
        .put("kty", "oct")
        .put("k", Base64.getUrlEncoder().encodeToString("notasecret".getBytes(StandardCharsets.UTF_8))))
    );
    TokenCredentials authInfo = new TokenCredentials(token);
    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGenerateNewTokenForceAlgorithm() {
    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("keystore.jceks")
        .setType("jceks")
        .setPassword("secret")));

    String token = authProvider.generateToken(new JsonObject(), new JWTOptions().setAlgorithm("RS256"));
    assertNotNull(token);

    // reverse
    TokenCredentials authInfo = new TokenCredentials(token);
    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testAcceptInvalidJWT() {
    String[] segments = JWT_INVALID.split("\\.");
    // All segment should be base64
    String headerSeg = segments[0];

    // change alg to none
    JsonObject headerJson = new JsonObject(new String(Base64.getUrlDecoder().decode(headerSeg.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    headerJson.put("alg", "none");
    headerSeg = Base64.getUrlEncoder().encodeToString(headerJson.encode().getBytes(StandardCharsets.UTF_8));

    // fix time exp
    String payloadSeg = segments[1];
    JsonObject bodyJson = new JsonObject(new String(Base64.getUrlDecoder().decode(payloadSeg.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    bodyJson.put("exp", System.currentTimeMillis() + 10000);
    payloadSeg = Base64.getUrlEncoder().encodeToString(headerJson.encode().getBytes(StandardCharsets.UTF_8));

    String signatureSeg = segments[2];

    // build attack token
    String attackerJWT = headerSeg + "." + payloadSeg + "." + signatureSeg;
    TokenCredentials authInfo = new TokenCredentials(attackerJWT);
    authProvider.authenticate(authInfo, onFailure(thr -> {
      assertNotNull(thr);
      testComplete();
    }));
    await();
  }

  @Test
  public void testAlgNone() {

    JWTAuth authProvider = JWTAuth.create(vertx, new JWTAuthOptions());

    JsonObject payload = new JsonObject()
      .put("sub", "UserUnderTest")
      .put("aud", "OrganizationUnderTest")
      .put("iat", 1431695313)
      .put("exp", 1747055313)
      .put("roles", new JsonArray().add("admin").add("developer").add("user"))
      .put("permissions", new JsonArray().add("read").add("write").add("execute"));

    final String token = authProvider.generateToken(payload, new JWTOptions().setSubject("UserUnderTest").setAlgorithm("none"));
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testLeeway() {
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(new JWTOptions().setLeeway(0)));

    long now = System.currentTimeMillis() / 1000;

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo")
      .put("exp", now);

    String token = authProvider.generateToken(payload);
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);
    // fail because exp is <= to now
    authProvider.authenticate(authInfo, onFailure(t -> testComplete()));
    await();
  }

  @Test
  public void testLeeway2() {
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(new JWTOptions().setLeeway(0)));

    long now = (System.currentTimeMillis() / 1000) + 2;

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo")
      .put("iat", now);

    String token = authProvider.generateToken(payload);
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);
    // fail because iat is > now (clock drifted 2 sec)
    authProvider.authenticate(authInfo, onFailure(t -> testComplete()));
    await();
  }

  @Test
  public void testLeeway3() {
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(new JWTOptions().setLeeway(5)));

    long now = System.currentTimeMillis() / 1000;

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo")
      .put("exp", now)
      .put("iat", now);

    String token = authProvider.generateToken(payload);
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);
    // fail because exp is <= to now
    authProvider.authenticate(authInfo, onSuccess(t -> testComplete()));
    await();
  }

  @Test
  public void testLeeway4() {
    authProvider = JWTAuth.create(vertx, getConfig().setJWTOptions(new JWTOptions().setLeeway(5)));

    long now = (System.currentTimeMillis() / 1000) + 2;

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo")
      .put("iat", now);

    String token = authProvider.generateToken(payload);
    assertNotNull(token);

    TokenCredentials authInfo = new TokenCredentials(token);
    // pass because iat is > now (clock drifted 2 sec) and we have a leeway of 5sec
    authProvider.authenticate(authInfo, onSuccess(t -> testComplete()));
    await();
  }

  @Test
  public void testJWKShouldNotCrash() {

    authProvider = JWTAuth.create(vertx, new JWTAuthOptions().addJwk(
      new JsonObject()
        .put("kty", "RSA")
        .put("n", "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw")
        .put("e", "AQAB")
        .put("alg", "RS256")
        .put("kid", "2011-04-29")));

  }

  @Test
  public void testValidateTokenWithIgnoreExpired() throws InterruptedException {
    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addJwk(new JsonObject()
        .put("kty", "oct")
        .put("k", "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"))
      .setJWTOptions(new JWTOptions()
        .setIgnoreExpiration(true)));

    String token = authProvider
      .generateToken(
        new JsonObject(),
        new JWTOptions()
          .setExpiresInSeconds(1)
          .setSubject("subject")
          .setAlgorithm("HS256"));

    // force a sleep to invalidate the token
    Thread.sleep(1001);

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      testComplete();
    }));
    await();
  }

  @Test
  public void testGenerateClaimsAndCheck() {

    JsonObject payload = new JsonObject()
      .put("sub", "Paulo");

    String token = authProvider.generateToken(payload, new JWTOptions().addPermission("user"));

    TokenCredentials authInfo = new TokenCredentials(token);

    authProvider.authenticate(authInfo, onSuccess(res -> {
      assertNotNull(res);
      // the permission has been properly decoded from the legacy token
      assertTrue(PermissionBasedAuthorization.create("user").match(res));

      res.clearCache();

      // overwrite with the JWT decoder
      JWTAuthorization.create("permissions").getAuthorizations(res, permissions -> {
        assertTrue(permissions.succeeded());
        assertTrue(PermissionBasedAuthorization.create("user").match(res));
        testComplete();
      });
    }));
    await();
  }

}
