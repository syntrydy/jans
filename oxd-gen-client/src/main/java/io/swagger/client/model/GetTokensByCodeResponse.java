/*
 * oxd-server
 * oxd-server
 *
 * OpenAPI spec version: 4.0.0
 * Contact: yuriyz@gluu.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.GetTokensByCodeResponseIdTokenClaims;
import java.io.IOException;

/**
 * GetTokensByCodeResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-10-10T07:48:52.952Z")
public class GetTokensByCodeResponse {
  @SerializedName("access_token")
  private String accessToken = null;

  @SerializedName("expires_in")
  private Integer expiresIn = null;

  @SerializedName("id_token")
  private String idToken = null;

  @SerializedName("refresh_token")
  private String refreshToken = null;

  @SerializedName("id_token_claims")
  private GetTokensByCodeResponseIdTokenClaims idTokenClaims = null;

  public GetTokensByCodeResponse accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

   /**
   * Get accessToken
   * @return accessToken
  **/
  @ApiModelProperty(example = "b75434ff-f465-4b70-92e4-b7ba6b6c58f2", required = true, value = "")
  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public GetTokensByCodeResponse expiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
    return this;
  }

   /**
   * Get expiresIn
   * @return expiresIn
  **/
  @ApiModelProperty(example = "299", required = true, value = "")
  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  public GetTokensByCodeResponse idToken(String idToken) {
    this.idToken = idToken;
    return this;
  }

   /**
   * Get idToken
   * @return idToken
  **/
  @ApiModelProperty(example = "eyJraWQiOiI5MTUyNTU1Ni04YmIwLTQ2MzYtYTFhYy05ZGVlNjlhMDBmYWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJp", required = true, value = "")
  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }

  public GetTokensByCodeResponse refreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

   /**
   * Get refreshToken
   * @return refreshToken
  **/
  @ApiModelProperty(example = "33d7988e-6ffb-4fe5-8c2a-0e158691d446", required = true, value = "")
  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public GetTokensByCodeResponse idTokenClaims(GetTokensByCodeResponseIdTokenClaims idTokenClaims) {
    this.idTokenClaims = idTokenClaims;
    return this;
  }

   /**
   * Get idTokenClaims
   * @return idTokenClaims
  **/
  @ApiModelProperty(required = true, value = "")
  public GetTokensByCodeResponseIdTokenClaims getIdTokenClaims() {
    return idTokenClaims;
  }

  public void setIdTokenClaims(GetTokensByCodeResponseIdTokenClaims idTokenClaims) {
    this.idTokenClaims = idTokenClaims;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetTokensByCodeResponse getTokensByCodeResponse = (GetTokensByCodeResponse) o;
    return Objects.equals(this.accessToken, getTokensByCodeResponse.accessToken) &&
        Objects.equals(this.expiresIn, getTokensByCodeResponse.expiresIn) &&
        Objects.equals(this.idToken, getTokensByCodeResponse.idToken) &&
        Objects.equals(this.refreshToken, getTokensByCodeResponse.refreshToken) &&
        Objects.equals(this.idTokenClaims, getTokensByCodeResponse.idTokenClaims);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, expiresIn, idToken, refreshToken, idTokenClaims);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetTokensByCodeResponse {\n");
    
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    expiresIn: ").append(toIndentedString(expiresIn)).append("\n");
    sb.append("    idToken: ").append(toIndentedString(idToken)).append("\n");
    sb.append("    refreshToken: ").append(toIndentedString(refreshToken)).append("\n");
    sb.append("    idTokenClaims: ").append(toIndentedString(idTokenClaims)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

