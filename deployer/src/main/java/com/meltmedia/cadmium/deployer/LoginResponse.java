package com.meltmedia.cadmium.deployer;

public class LoginResponse {
  protected String userName;
  protected String accessToken;
  
  public String getUserName() {
    return userName;
  }
  public void setUserName(String userName) {
    this.userName = userName;
  }
  public String getAccessToken() {
    return accessToken;
  }
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

}
