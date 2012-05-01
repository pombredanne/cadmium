package com.meltmedia.cadmium.jgroups;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JChannelProvider implements Provider<JChannel> {
  
  public static final String CHANNEL_NAME = "JGroupsName";
  public static final String CONFIG_NAME = "JGroupsConfigName";
  
  private final Logger log = LoggerFactory.getLogger(getClass());

  private String channelName;
  private URL configFile;
  
  @Inject
  public JChannelProvider(@Named(CHANNEL_NAME) String channelName, @Named(CONFIG_NAME) URL configFile) {
    this.channelName = channelName;
    this.configFile = configFile;
  }
  
  @Override
  public JChannel get() {
    try{
      JChannel channel = new JChannel(configFile);
      channel.connect(channelName);
      return channel;
    } catch(Exception e) {
      log.error("Failed to get jgroups channel", e);
    }
    return null;
  }

}
