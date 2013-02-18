package com.meltmedia.cadmium.deployer;

public class AccessTokenRequest {
  protected String code;
  
  public void setCode( String code ) {
    this.code = code;
  }
  
  public String getCode() {
    return this.code;
  }
}
