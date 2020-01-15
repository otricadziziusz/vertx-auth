package io.vertx.ext.auth.webauthn;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.ext.auth.webauthn.WebAuthnOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.auth.webauthn.WebAuthnOptions} original class using Vert.x codegen.
 */
public class WebAuthnOptionsConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, WebAuthnOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "attestation":
          if (member.getValue() instanceof String) {
            obj.setAttestation(io.vertx.ext.auth.webauthn.Attestation.valueOf((String)member.getValue()));
          }
          break;
        case "authenticatorAttachment":
          if (member.getValue() instanceof String) {
            obj.setAuthenticatorAttachment(io.vertx.ext.auth.webauthn.AuthenticatorAttachment.valueOf((String)member.getValue()));
          }
          break;
        case "authenticatorSelection":
          break;
        case "challengeLength":
          if (member.getValue() instanceof Number) {
            obj.setChallengeLength(((Number)member.getValue()).intValue());
          }
          break;
        case "origin":
          if (member.getValue() instanceof String) {
            obj.setOrigin((String)member.getValue());
          }
          break;
        case "pubKeyCredParams":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<java.lang.String> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setPubKeyCredParams(list);
          }
          break;
        case "relayParty":
          if (member.getValue() instanceof JsonObject) {
            obj.setRelayParty(new io.vertx.ext.auth.webauthn.RelayParty((JsonObject)member.getValue()));
          }
          break;
        case "requireResidentKey":
          if (member.getValue() instanceof Boolean) {
            obj.setRequireResidentKey((Boolean)member.getValue());
          }
          break;
        case "timeout":
          if (member.getValue() instanceof Number) {
            obj.setTimeout(((Number)member.getValue()).intValue());
          }
          break;
        case "transports":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<java.lang.String> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setTransports(list);
          }
          break;
        case "userVerification":
          if (member.getValue() instanceof String) {
            obj.setUserVerification(io.vertx.ext.auth.webauthn.UserVerification.valueOf((String)member.getValue()));
          }
          break;
      }
    }
  }

  public static void toJson(WebAuthnOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(WebAuthnOptions obj, java.util.Map<String, Object> json) {
    if (obj.getAttestation() != null) {
      json.put("attestation", obj.getAttestation().name());
    }
    if (obj.getAuthenticatorAttachment() != null) {
      json.put("authenticatorAttachment", obj.getAuthenticatorAttachment().name());
    }
    if (obj.getAuthenticatorSelection() != null) {
      json.put("authenticatorSelection", obj.getAuthenticatorSelection());
    }
    json.put("challengeLength", obj.getChallengeLength());
    if (obj.getOrigin() != null) {
      json.put("origin", obj.getOrigin());
    }
    if (obj.getPubKeyCredParams() != null) {
      JsonArray array = new JsonArray();
      obj.getPubKeyCredParams().forEach(item -> array.add(item));
      json.put("pubKeyCredParams", array);
    }
    if (obj.getRelayParty() != null) {
      json.put("relayParty", obj.getRelayParty().toJson());
    }
    if (obj.getRequireResidentKey() != null) {
      json.put("requireResidentKey", obj.getRequireResidentKey());
    }
    json.put("timeout", obj.getTimeout());
    if (obj.getTransports() != null) {
      JsonArray array = new JsonArray();
      obj.getTransports().forEach(item -> array.add(item));
      json.put("transports", array);
    }
    if (obj.getUserVerification() != null) {
      json.put("userVerification", obj.getUserVerification().name());
    }
  }
}
