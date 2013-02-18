/**
 *    Copyright 2012 meltmedia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.meltmedia.cadmium.deployer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.http.HttpResponse;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.meltmedia.cadmium.core.CadmiumSystemEndpoint;

@CadmiumSystemEndpoint
@Path("/github/accessToken")
public class GitHubOAuth3Service {
  private static Gson gson = new GsonBuilder()
  .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
  .create();
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public AccessTokenResponse getAuthToken(AccessTokenRequest authTokenRequest) {
    DefaultHttpClient client = new DefaultHttpClient();

    HttpPost post = new HttpPost("https://github.com/login/oauth/access_token");
    post.setHeader("Content-Type", MediaType.APPLICATION_JSON);
    post.setHeader("Accept", MediaType.APPLICATION_JSON);
    try {
      post.setEntity(new StringEntity(gson.toJson(authTokenRequest)));
    } catch (UnsupportedEncodingException e) {
      throw new WebApplicationException(Response.serverError().build());
    }

    org.apache.http.HttpResponse response;
    try {
      response = client.execute(post);
    } catch (Exception e) {
      throw new WebApplicationException(Response.serverError().build());
    }
    if( response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ) {
      HttpEntity entity = response.getEntity();
      String resp;
      try {
        resp = EntityUtils.toString(entity);
      } catch (Exception e) {
        throw new WebApplicationException(Response.serverError().build());
      }
      return gson.fromJson(resp, AccessTokenResponse.class);
    }
    else {
      throw new WebApplicationException(response.getStatusLine().getStatusCode());
    }
  }
}
