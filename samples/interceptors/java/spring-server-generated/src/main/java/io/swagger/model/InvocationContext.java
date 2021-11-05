package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.InvocationContextAuthenticationContext;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * InvocationContext
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-04T06:10:22.979Z[GMT]")


public class InvocationContext   {
  @JsonProperty("requestId")
  private String requestId = null;

  @JsonProperty("protocol")
  private String protocol = null;

  @JsonProperty("scheme")
  private String scheme = null;

  @JsonProperty("apiName")
  private String apiName = null;

  @JsonProperty("apiVersion")
  private String apiVersion = null;

  @JsonProperty("vhost")
  private String vhost = null;

  @JsonProperty("supportedMethods")
  private String supportedMethods = null;

  @JsonProperty("method")
  private String method = null;

  @JsonProperty("basePath")
  private String basePath = null;

  @JsonProperty("path")
  private String path = null;

  @JsonProperty("pathTemplate")
  private String pathTemplate = null;

  @JsonProperty("source")
  private String source = null;

  @JsonProperty("prodClusterName")
  private String prodClusterName = null;

  @JsonProperty("sandClusterName")
  private String sandClusterName = null;

  @JsonProperty("authenticationContext")
  private InvocationContextAuthenticationContext authenticationContext = null;

  public InvocationContext requestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  /**
   * Get requestId
   * @return requestId
   **/
  @Schema(example = "75269e44-f797-4432-9906-cf39e68d6ab8", description = "")
  
    public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public InvocationContext protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  /**
   * Get protocol
   * @return protocol
   **/
  @Schema(example = "HTTP/1.1", description = "")
  
    public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public InvocationContext scheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  /**
   * Get scheme
   * @return scheme
   **/
  @Schema(example = "https", description = "")
  
    public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public InvocationContext apiName(String apiName) {
    this.apiName = apiName;
    return this;
  }

  /**
   * Get apiName
   * @return apiName
   **/
  @Schema(example = "PetStore", description = "")
  
    public String getApiName() {
    return apiName;
  }

  public void setApiName(String apiName) {
    this.apiName = apiName;
  }

  public InvocationContext apiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  /**
   * Get apiVersion
   * @return apiVersion
   **/
  @Schema(example = "v1.0.0", description = "")
  
    public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public InvocationContext vhost(String vhost) {
    this.vhost = vhost;
    return this;
  }

  /**
   * Get vhost
   * @return vhost
   **/
  @Schema(example = "localhost", description = "")
  
    public String getVhost() {
    return vhost;
  }

  public void setVhost(String vhost) {
    this.vhost = vhost;
  }

  public InvocationContext supportedMethods(String supportedMethods) {
    this.supportedMethods = supportedMethods;
    return this;
  }

  /**
   * Get supportedMethods
   * @return supportedMethods
   **/
  @Schema(example = "GET POST", description = "")
  
    public String getSupportedMethods() {
    return supportedMethods;
  }

  public void setSupportedMethods(String supportedMethods) {
    this.supportedMethods = supportedMethods;
  }

  public InvocationContext method(String method) {
    this.method = method;
    return this;
  }

  /**
   * Get method
   * @return method
   **/
  @Schema(example = "POST", description = "")
  
    public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public InvocationContext basePath(String basePath) {
    this.basePath = basePath;
    return this;
  }

  /**
   * Get basePath
   * @return basePath
   **/
  @Schema(example = "/petstore", description = "")
  
    public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public InvocationContext path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Get path
   * @return path
   **/
  @Schema(example = "/petstore/pet/1", description = "")
  
    public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public InvocationContext pathTemplate(String pathTemplate) {
    this.pathTemplate = pathTemplate;
    return this;
  }

  /**
   * Get pathTemplate
   * @return pathTemplate
   **/
  @Schema(example = "/pet/{petID}", description = "")
  
    public String getPathTemplate() {
    return pathTemplate;
  }

  public void setPathTemplate(String pathTemplate) {
    this.pathTemplate = pathTemplate;
  }

  public InvocationContext source(String source) {
    this.source = source;
    return this;
  }

  /**
   * Get source
   * @return source
   **/
  @Schema(example = "192.168.8.332:8080", description = "")
  
    public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public InvocationContext prodClusterName(String prodClusterName) {
    this.prodClusterName = prodClusterName;
    return this;
  }

  /**
   * Get prodClusterName
   * @return prodClusterName
   **/
  @Schema(example = "carbon.super_clusterProd_localhost_Online-Storev1.0.0", description = "")
  
    public String getProdClusterName() {
    return prodClusterName;
  }

  public void setProdClusterName(String prodClusterName) {
    this.prodClusterName = prodClusterName;
  }

  public InvocationContext sandClusterName(String sandClusterName) {
    this.sandClusterName = sandClusterName;
    return this;
  }

  /**
   * Get sandClusterName
   * @return sandClusterName
   **/
  @Schema(description = "")
  
    public String getSandClusterName() {
    return sandClusterName;
  }

  public void setSandClusterName(String sandClusterName) {
    this.sandClusterName = sandClusterName;
  }

  public InvocationContext authenticationContext(InvocationContextAuthenticationContext authenticationContext) {
    this.authenticationContext = authenticationContext;
    return this;
  }

  /**
   * Get authenticationContext
   * @return authenticationContext
   **/
  @Schema(description = "")
  
    @Valid
    public InvocationContextAuthenticationContext getAuthenticationContext() {
    return authenticationContext;
  }

  public void setAuthenticationContext(InvocationContextAuthenticationContext authenticationContext) {
    this.authenticationContext = authenticationContext;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvocationContext invocationContext = (InvocationContext) o;
    return Objects.equals(this.requestId, invocationContext.requestId) &&
        Objects.equals(this.protocol, invocationContext.protocol) &&
        Objects.equals(this.scheme, invocationContext.scheme) &&
        Objects.equals(this.apiName, invocationContext.apiName) &&
        Objects.equals(this.apiVersion, invocationContext.apiVersion) &&
        Objects.equals(this.vhost, invocationContext.vhost) &&
        Objects.equals(this.supportedMethods, invocationContext.supportedMethods) &&
        Objects.equals(this.method, invocationContext.method) &&
        Objects.equals(this.basePath, invocationContext.basePath) &&
        Objects.equals(this.path, invocationContext.path) &&
        Objects.equals(this.pathTemplate, invocationContext.pathTemplate) &&
        Objects.equals(this.source, invocationContext.source) &&
        Objects.equals(this.prodClusterName, invocationContext.prodClusterName) &&
        Objects.equals(this.sandClusterName, invocationContext.sandClusterName) &&
        Objects.equals(this.authenticationContext, invocationContext.authenticationContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, protocol, scheme, apiName, apiVersion, vhost, supportedMethods, method, basePath, path, pathTemplate, source, prodClusterName, sandClusterName, authenticationContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvocationContext {\n");
    
    sb.append("    requestId: ").append(toIndentedString(requestId)).append("\n");
    sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");
    sb.append("    scheme: ").append(toIndentedString(scheme)).append("\n");
    sb.append("    apiName: ").append(toIndentedString(apiName)).append("\n");
    sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
    sb.append("    vhost: ").append(toIndentedString(vhost)).append("\n");
    sb.append("    supportedMethods: ").append(toIndentedString(supportedMethods)).append("\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    basePath: ").append(toIndentedString(basePath)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    pathTemplate: ").append(toIndentedString(pathTemplate)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    prodClusterName: ").append(toIndentedString(prodClusterName)).append("\n");
    sb.append("    sandClusterName: ").append(toIndentedString(sandClusterName)).append("\n");
    sb.append("    authenticationContext: ").append(toIndentedString(authenticationContext)).append("\n");
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
