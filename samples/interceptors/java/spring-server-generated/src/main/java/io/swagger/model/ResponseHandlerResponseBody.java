package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.HeaderKeys;
import io.swagger.model.Headers;
import io.swagger.model.Trailers;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ResponseHandlerResponseBody
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-04T06:10:22.979Z[GMT]")


public class ResponseHandlerResponseBody   {
  @JsonProperty("responseCode")
  private Integer responseCode = null;

  @JsonProperty("headersToAdd")
  private Headers headersToAdd = null;

  @JsonProperty("headersToReplace")
  private Headers headersToReplace = null;

  @JsonProperty("headersToRemove")
  private HeaderKeys headersToRemove = null;

  @JsonProperty("trailersToAdd")
  private Trailers trailersToAdd = null;

  @JsonProperty("trailersToReplace")
  private Trailers trailersToReplace = null;

  @JsonProperty("trailersToRemove")
  private HeaderKeys trailersToRemove = null;

  @JsonProperty("body")
  private String body = null;

  public ResponseHandlerResponseBody responseCode(Integer responseCode) {
    this.responseCode = responseCode;
    return this;
  }

  /**
   * Get responseCode
   * @return responseCode
   **/
  @Schema(example = "200", description = "")
  
    public Integer getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(Integer responseCode) {
    this.responseCode = responseCode;
  }

  public ResponseHandlerResponseBody headersToAdd(Headers headersToAdd) {
    this.headersToAdd = headersToAdd;
    return this;
  }

  /**
   * Get headersToAdd
   * @return headersToAdd
   **/
  @Schema(description = "")
  
    @Valid
    public Headers getHeadersToAdd() {
    return headersToAdd;
  }

  public void setHeadersToAdd(Headers headersToAdd) {
    this.headersToAdd = headersToAdd;
  }

  public ResponseHandlerResponseBody headersToReplace(Headers headersToReplace) {
    this.headersToReplace = headersToReplace;
    return this;
  }

  /**
   * Get headersToReplace
   * @return headersToReplace
   **/
  @Schema(description = "")
  
    @Valid
    public Headers getHeadersToReplace() {
    return headersToReplace;
  }

  public void setHeadersToReplace(Headers headersToReplace) {
    this.headersToReplace = headersToReplace;
  }

  public ResponseHandlerResponseBody headersToRemove(HeaderKeys headersToRemove) {
    this.headersToRemove = headersToRemove;
    return this;
  }

  /**
   * Get headersToRemove
   * @return headersToRemove
   **/
  @Schema(description = "")
  
    @Valid
    public HeaderKeys getHeadersToRemove() {
    return headersToRemove;
  }

  public void setHeadersToRemove(HeaderKeys headersToRemove) {
    this.headersToRemove = headersToRemove;
  }

  public ResponseHandlerResponseBody trailersToAdd(Trailers trailersToAdd) {
    this.trailersToAdd = trailersToAdd;
    return this;
  }

  /**
   * Get trailersToAdd
   * @return trailersToAdd
   **/
  @Schema(description = "")
  
    @Valid
    public Trailers getTrailersToAdd() {
    return trailersToAdd;
  }

  public void setTrailersToAdd(Trailers trailersToAdd) {
    this.trailersToAdd = trailersToAdd;
  }

  public ResponseHandlerResponseBody trailersToReplace(Trailers trailersToReplace) {
    this.trailersToReplace = trailersToReplace;
    return this;
  }

  /**
   * Get trailersToReplace
   * @return trailersToReplace
   **/
  @Schema(description = "")
  
    @Valid
    public Trailers getTrailersToReplace() {
    return trailersToReplace;
  }

  public void setTrailersToReplace(Trailers trailersToReplace) {
    this.trailersToReplace = trailersToReplace;
  }

  public ResponseHandlerResponseBody trailersToRemove(HeaderKeys trailersToRemove) {
    this.trailersToRemove = trailersToRemove;
    return this;
  }

  /**
   * Get trailersToRemove
   * @return trailersToRemove
   **/
  @Schema(description = "")
  
    @Valid
    public HeaderKeys getTrailersToRemove() {
    return trailersToRemove;
  }

  public void setTrailersToRemove(HeaderKeys trailersToRemove) {
    this.trailersToRemove = trailersToRemove;
  }

  public ResponseHandlerResponseBody body(String body) {
    this.body = body;
    return this;
  }

  /**
   * Get body
   * @return body
   **/
  @Schema(description = "")
  
    public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResponseHandlerResponseBody responseHandlerResponseBody = (ResponseHandlerResponseBody) o;
    return Objects.equals(this.responseCode, responseHandlerResponseBody.responseCode) &&
        Objects.equals(this.headersToAdd, responseHandlerResponseBody.headersToAdd) &&
        Objects.equals(this.headersToReplace, responseHandlerResponseBody.headersToReplace) &&
        Objects.equals(this.headersToRemove, responseHandlerResponseBody.headersToRemove) &&
        Objects.equals(this.trailersToAdd, responseHandlerResponseBody.trailersToAdd) &&
        Objects.equals(this.trailersToReplace, responseHandlerResponseBody.trailersToReplace) &&
        Objects.equals(this.trailersToRemove, responseHandlerResponseBody.trailersToRemove) &&
        Objects.equals(this.body, responseHandlerResponseBody.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseCode, headersToAdd, headersToReplace, headersToRemove, trailersToAdd, trailersToReplace, trailersToRemove, body);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResponseHandlerResponseBody {\n");
    
    sb.append("    responseCode: ").append(toIndentedString(responseCode)).append("\n");
    sb.append("    headersToAdd: ").append(toIndentedString(headersToAdd)).append("\n");
    sb.append("    headersToReplace: ").append(toIndentedString(headersToReplace)).append("\n");
    sb.append("    headersToRemove: ").append(toIndentedString(headersToRemove)).append("\n");
    sb.append("    trailersToAdd: ").append(toIndentedString(trailersToAdd)).append("\n");
    sb.append("    trailersToReplace: ").append(toIndentedString(trailersToReplace)).append("\n");
    sb.append("    trailersToRemove: ").append(toIndentedString(trailersToRemove)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
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
