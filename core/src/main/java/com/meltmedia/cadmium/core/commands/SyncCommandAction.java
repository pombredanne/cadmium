package com.meltmedia.cadmium.core.commands;

import java.util.Properties;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meltmedia.cadmium.core.CommandAction;
import com.meltmedia.cadmium.core.CommandContext;
import com.meltmedia.cadmium.core.ContentService;
import com.meltmedia.cadmium.core.CoordinatedWorker;
import com.meltmedia.cadmium.core.CoordinatedWorkerListener;
import com.meltmedia.cadmium.core.SiteDownService;
import com.meltmedia.cadmium.core.messaging.ChannelMember;
import com.meltmedia.cadmium.core.messaging.MembershipTracker;
import com.meltmedia.cadmium.core.messaging.Message;
import com.meltmedia.cadmium.core.messaging.MessageSender;
import com.meltmedia.cadmium.core.messaging.ProtocolMessage;

public class SyncCommandAction implements CommandAction {
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  @Inject
  protected CoordinatedWorker worker;
  
  @Inject
  protected MembershipTracker tracker;
  
  @Inject
  protected Properties configProperties;
  
  @Inject
  protected MessageSender sender;
  
  @Inject
  protected SiteDownService maintFilter;
  
  @Inject
  protected ContentService fileServlet;

  @Override
  public boolean execute(CommandContext ctx) throws Exception {
    if(!tracker.getCoordinator().isMine()) {
      handleCommandAsNonCoordinator(ctx);
    } else {
      handleCommandAsCoordinator(ctx);
    }
    return true;
  }

  private void handleCommandAsNonCoordinator(CommandContext ctx) {
    log.info("Received SYNC request from coordinator");
    boolean update = false;
    if(ctx.getMessage().getProtocolParameters().containsKey("branch") || ctx.getMessage().getProtocolParameters().containsKey("sha")) {
      update = true;
    }
    
    if(update) {
      log.info("Taking site down to run sync update!");
      maintFilter.start();
      final CoordinatedWorkerListener oldListener = worker.getListener();
      worker.setListener(new CoordinatedWorkerListener() {

        @Override
        public void workDone() {
          log.info("Sync done");
          fileServlet.switchContent();
          maintFilter.stop();
          worker.setListener(oldListener);
        }

        @Override
        public void workFailed() {
          log.info("Sync failed");
          worker.setListener(oldListener);
        }
      });
      
      this.worker.beginPullUpdates(ctx.getMessage().getProtocolParameters());
    }
  }

  private void handleCommandAsCoordinator(CommandContext ctx) {
    log.info("Received SYNC message from new member {}", ctx.getSource());
    boolean update = false;
    if(ctx.getMessage().getProtocolParameters().containsKey("branch") && ctx.getMessage().getProtocolParameters().containsKey("sha")) {
      if(configProperties.containsKey("branch") && configProperties.containsKey("git.ref.sha")) {
        if(!configProperties.getProperty("branch").equals(ctx.getMessage().getProtocolParameters().get("branch")) || !configProperties.getProperty("git.ref.sha").equals(ctx.getMessage().getProtocolParameters().get("sha"))) {
          update = true;
        }
      }
    } else if (configProperties.containsKey("branch") && configProperties.containsKey("git.ref.sha")) {
      update = true;
    }
    
    if(update) {
      Message syncMessage = new Message();
      syncMessage.setCommand(ProtocolMessage.SYNC);
      syncMessage.getProtocolParameters().put("branch", configProperties.getProperty("branch"));
      syncMessage.getProtocolParameters().put("sha", configProperties.getProperty("git.ref.sha"));
      try{
        log.info("Sending SYNC message to new member {}, branch {}, sha {}", new Object[] {ctx.getSource(), configProperties.getProperty("branch"), configProperties.getProperty("git.ref.sha")});
        sender.sendMessage(syncMessage, new ChannelMember(ctx.getSource()));
      } catch(Exception e) {
        log.warn("Failed to send SYNC message", e);
      }
    }
  }

  @Override
  public void handleFailure(CommandContext ctx, Exception e) {
    
  }

}
