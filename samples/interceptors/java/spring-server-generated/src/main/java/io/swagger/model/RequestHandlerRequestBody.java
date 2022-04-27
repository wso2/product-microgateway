package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.Headers;
import io.swagger.model.InvocationContext;
import io.swagger.model.Trailers;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * RequestHandlerRequestBody
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-04T06:10:22.979Z[GMT]")


public class RequestHandlerRequestBody   {
  @JsonProperty("requestHeaders")
  private Headers requestHeaders = null;

  @JsonProperty("requestTrailers")
  private Trailers requestTrailers = null;

  @JsonProperty("requestBody")
  private String requestBody = null;

  @JsonProperty("invocationContext")
  private InvocationContext invocationContext = null;

  public RequestHandlerRequestBody requestHeaders(Headers requestHeaders) {
    this.requestHeaders = requestHeaders;
    return this;
  }

  /**
   * Get requestHeaders
   * @return requestHeaders
   **/
  @Schema(description = "")
  
    @Valid
    public Headers getRequestHeaders() {
    return requestHeaders;
  }

  public void setRequestHeaders(Headers requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  public RequestHandlerRequestBody requestTrailers(Trailers requestTrailers) {
    this.requestTrailers = requestTrailers;
    return this;
  }

  /**
   * Get requestTrailers
   * @return requestTrailers
   **/
  @Schema(description = "")
  
    @Valid
    public Trailers getRequestTrailers() {
    return requestTrailers;
  }

  public void setRequestTrailers(Trailers requestTrailers) {
    this.requestTrailers = requestTrailers;
  }

  public RequestHandlerRequestBody requestBody(String requestBody) {
    this.requestBody = requestBody;
    return this;
  }

  /**
   * Get requestBody
   * @return requestBody
   **/
  @Schema(description = "")
  
    public String getRequestBody() {
    return requestBody;
  }

  public void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  public RequestHandlerRequestBody invocationContext(InvocationContext invocationContext) {
    this.invocationContext = invocationContext;
    return this;
  }

  /**
   * Get invocationContext
   * @return invocationContext
   **/
  @Schema(description = "")
  
    @Valid
    public InvocationContext getInvocationContext() {
    return invocationContext;
  }

  public void setInvocationContext(InvocationContext invocationContext) {
    this.invocationContext = invocationContext;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RequestHandlerRequestBody requestHandlerRequestBody = (RequestHandlerRequestBody) o;
    return Objects.equals(this.requestHeaders, requestHandlerRequestBody.requestHeaders) &&
        Objects.equals(this.requestTrailers, requestHandlerRequestBody.requestTrailers) &&
        Objects.equals(this.requestBody, requestHandlerRequestBody.requestBody) &&
        Objects.equals(this.invocationContext, requestHandlerRequestBody.invocationContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestHeaders, requestTrailers, requestBody, invocationContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RequestHandlerRequestBody {\n");
    
    sb.append("    requestHeaders: ").append(toIndentedString(requestHeaders)).append("\n");
    sb.append("    requestTrailers: ").append(toIndentedString(requestTrailers)).append("\n");
    sb.append("    requestBody: ").append(toIndentedString(requestBody)).append("\n");
    sb.append("    invocationContext: ").append(toIndentedString(invocationContext)).append("\n");
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
