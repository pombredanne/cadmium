package com.meltmedia.cadmium.deployer;

import com.meltmedia.cadmium.core.config.CadmiumConfig;

@CadmiumConfig(GitHubApiConfig.KEY)
public class GitHubApiConfig {
  public static final String KEY = "GitHubApi";
  
  public String clientSecret;
  public String clientId;
  
  public String getClientSecret() {
    return clientSecret;
  }
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }
  public String getClientId() {
    return clientId;
  }
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }
  
  
}
