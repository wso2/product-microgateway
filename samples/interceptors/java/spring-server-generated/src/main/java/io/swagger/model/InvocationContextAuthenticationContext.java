package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * InvocationContextAuthenticationContext
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-04T06:10:22.979Z[GMT]")


public class InvocationContextAuthenticationContext   {
  @JsonProperty("tokenType")
  private String tokenType = null;

  @JsonProperty("token")
  private String token = null;

  @JsonProperty("keyType")
  private String keyType = null;

  public InvocationContextAuthenticationContext tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  /**
   * Get tokenType
   * @return tokenType
   **/
  @Schema(example = "JWT", description = "")
  
    public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public InvocationContextAuthenticationContext token(String token) {
    this.token = token;
    return this;
  }

  /**
   * Get token
   * @return token
   **/
  @Schema(example = "xxxxxx-xxxx-xxxxxx-xxxx", description = "")
  
    public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public InvocationContextAuthenticationContext keyType(String keyType) {
    this.keyType = keyType;
    return this;
  }

  /**
   * Get keyType
   * @return keyType
   **/
  @Schema(example = "PRODUCTION", description = "")
  
    public String getKeyType() {
    return keyType;
  }

  public void setKeyType(String keyType) {
    this.keyType = keyType;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvocationContextAuthenticationContext invocationContextAuthenticationContext = (InvocationContextAuthenticationContext) o;
    return Objects.equals(this.tokenType, invocationContextAuthenticationContext.tokenType) &&
        Objects.equals(this.token, invocationContextAuthenticationContext.token) &&
        Objects.equals(this.keyType, invocationContextAuthenticationContext.keyType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokenType, token, keyType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvocationContextAuthenticationContext {\n");
    
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    keyType: ").append(toIndentedString(keyType)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
