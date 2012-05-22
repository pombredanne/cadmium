package com.meltmedia.cadmium.demos.basic;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContextEvent;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.meltmedia.cadmium.jgit.impl.CoordinatedWorkerImpl;
import com.meltmedia.cadmium.jgit.impl.GithubConfigSessionFactory;
import com.meltmedia.cadmium.jgroups.ContentService;
import com.meltmedia.cadmium.jgroups.CoordinatedWorker;
import com.meltmedia.cadmium.jgroups.JChannelProvider;
import com.meltmedia.cadmium.jgroups.SiteDownService;
import com.meltmedia.cadmium.jgroups.jersey.UpdateService;
import com.meltmedia.cadmium.jgroups.receivers.UpdateChannelReceiver;
import com.meltmedia.cadmium.servlets.FileServlet;
import com.meltmedia.cadmium.servlets.MaintenanceFilter;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * Builds the context with the Guice framework.  To see how this works, go to:
 * http://code.google.com/p/google-guice/wiki/ServletModule
 * 
 * @author Christian Trimble
 */

public class DemoListener extends GuiceServletContextListener {
  private final Logger log = LoggerFactory.getLogger(getClass());
	
  public static final String CONFIG_PROPERTIES_FILE = "config.properties";
  public static final String BASE_PATH_ENV = "com.meltmedia.cadmium.contentRoot";
  public static final String SSH_PATH_ENV = "com.meltmedia.cadmium.github.sshKey";
  public static final String LAST_UPDATED_DIR = "com.meltmedia.cadmium.lastUpdated";
  public File sharedContentRoot;
  public File applicationContentRoot;
  private String contentRootPath;
  private String repoDir = "git-checkout";
  private String contentDir = "renderedContent";
  private File sshDir;
  
	Injector injector = null;

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		try {
		  JChannel channel = injector.getInstance(JChannel.class);
		  channel.close();
		}
		catch( Exception e ) {
			
		}
		super.contextDestroyed(event);
	}

	@Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
	  Properties configProperties = new Properties();
      configProperties.putAll(System.getenv());
      configProperties.putAll(System.getProperties());
    
    if(configProperties.containsKey(BASE_PATH_ENV) ) {
    	sharedContentRoot = new File(configProperties.getProperty(BASE_PATH_ENV));
      if(!sharedContentRoot.exists() || !sharedContentRoot.canRead() || !sharedContentRoot.canWrite()) {
        sharedContentRoot = null;
      }
    }
    
    if( sharedContentRoot == null ) {
    	log.warn("Could not access cadmium content root.  Using the tempdir.");
    	sharedContentRoot = (File)servletContextEvent.getServletContext().getAttribute("javax.servlet.context.tempdir");
    }
    
    // compute the directory for this application, based on the war name.
    String path;
		path = servletContextEvent.getServletContext().getRealPath("/WEB-INF/web.xml");
		String[] pathSegments = path.split("/");
		String warName = pathSegments[pathSegments.length - 3];
      applicationContentRoot = new File(sharedContentRoot, warName);
      if( !applicationContentRoot.exists() ) applicationContentRoot.mkdir();
	
	if( applicationContentRoot == null ) {
		throw new RuntimeException("Could not make application content root.");
	}
	else {
		log.info("Application content root:"+applicationContentRoot.getAbsolutePath());
	}
    
    if( configProperties.containsKey(SSH_PATH_ENV)) {
    	sshDir = new File(configProperties.getProperty(SSH_PATH_ENV));
    	if( !sshDir.exists() && !sshDir.isDirectory()) {
    		sshDir = null;
    	}
    }
    if( sshDir == null ) {
    	sshDir = new File(sharedContentRoot, ".ssh");
	    if( !sshDir.exists() && !sshDir.isDirectory()) {
	    	sshDir = null;
	    }
    }
    
	if( sshDir != null ) {
      SshSessionFactory.setInstance(new GithubConfigSessionFactory(sshDir.getAbsolutePath()));
	}
    
	  String repoDir = servletContextEvent.getServletContext().getInitParameter("repoDir");
	  if(repoDir != null && repoDir.trim().length() > 0) {
	    this.repoDir = repoDir;
	  }
	  String contentDir = servletContextEvent.getServletContext().getInitParameter("contentDir");
	  if(contentDir != null && contentDir.trim().length() > 0) {
	    this.contentDir = contentDir;
	  }
    File repoFile = new File(this.applicationContentRoot, this.repoDir);
    this.repoDir = repoFile.getAbsoluteFile().getAbsolutePath();
    File contentFile = new File(this.applicationContentRoot, this.contentDir);
    this.contentDir = contentFile.getAbsoluteFile().getAbsolutePath();
    
    try {
    initRepoDir(repoFile);
    initContentDir(repoFile, contentFile);
    }
    catch( Exception e ) {
      log.warn("Failed to init repository directory or content directory.", e);
    }
    
    injector = Guice.createInjector(createServletModule());
    
    super.contextInitialized(servletContextEvent);
  }

  private void initContentDir(File repoFile, File contentFile) throws IOException {
    if(contentFile.exists() && contentFile.isDirectory() && contentFile.canWrite()) {
      this.contentDir = contentFile.getAbsoluteFile().getAbsolutePath();
    }
    else if( !contentFile.exists() ) {
	    CloneCommand clone = Git.cloneRepository();
	    clone.setCloneAllBranches(false);
	    clone.setCloneSubmodules(false);
	    clone.setDirectory(contentFile);
	    clone.setURI(new File(repoFile, ".git").getAbsolutePath());
	    Git git = clone.call();
      git.getRepository().close();
			FileUtils.deleteDirectory(new File(contentFile, ".git"));
    }
    else {
    	log.warn("The content directory exists, but we cannot write to it.");
    }
  }

  private void initRepoDir(File repoFile) {
    if(!repoFile.exists()) {
    	  CloneCommand cloneCommand = Git.cloneRepository()
    		        .setBare(false)
    				.setURI("git@github.com:meltmedia/cadmium-static-content-example.git")
    				.setBranch("HEAD")
    				.setDirectory(repoFile)
    				.setTimeout(60);
    		Git git = cloneCommand.call();
    		git.getRepository().close();
    }
  }

  @Override
	protected Injector getInjector() {
	  return injector;
	}
	
	private ServletModule createServletModule() {
      return new ServletModule() {
        @Override
		protected void configureServlets() {
          
          Properties configProperties = new Properties();
          configProperties.putAll(System.getenv());
          configProperties.putAll(System.getProperties());
          
          if(new File(applicationContentRoot, CONFIG_PROPERTIES_FILE).exists()) {
            try{
              configProperties.load(new FileReader(new File(applicationContentRoot, CONFIG_PROPERTIES_FILE)));
            } catch(Exception e){
              log.warn("Failed to load properties file ["+CONFIG_PROPERTIES_FILE+"] from content directory.", e);
            }
          }
          
          if(configProperties.containsKey(LAST_UPDATED_DIR)) {
            File cntDir = new File(configProperties.getProperty(LAST_UPDATED_DIR));
            if(cntDir.exists() && cntDir.canRead()) {
              contentDir = cntDir.getAbsolutePath();
            }
          }

          bind(MaintenanceFilter.class).in(Scopes.SINGLETON);
          bind(SiteDownService.class).to(MaintenanceFilter.class);
          
          bind(FileServlet.class).in(Scopes.SINGLETON);
          bind(ContentService.class).to(FileServlet.class);
          
          Map<String, String> fileParams = new HashMap<String, String>();
          fileParams.put("basePath", contentDir);
          
          Map<String, String> maintParams = new HashMap<String, String>();
          maintParams.put("ignorePrefix", "/system");

          serve("/system/*").with(GuiceContainer.class);
          
          serve("/*").with(FileServlet.class, fileParams);
          
          filter("/*").through(MaintenanceFilter.class, maintParams);

          //Bind application base path
          bind(String.class).annotatedWith(Names.named(UpdateChannelReceiver.BASE_PATH)).toInstance(applicationContentRoot.getAbsolutePath());
                    
          //Bind git repo path
          bind(String.class).annotatedWith(Names.named(UpdateService.REPOSITORY_LOCATION)).toInstance(repoDir);
          
          //Bind static content path
          bind(String.class).annotatedWith(Names.named(CoordinatedWorkerImpl.RENDERED_DIRECTORY)).toInstance(contentDir);
          
          //Bind channel name
          bind(String.class).annotatedWith(Names.named(JChannelProvider.CHANNEL_NAME)).toInstance("CadmiumChannel");
          
          if(configProperties.containsKey(SSH_PATH_ENV)) {
            bind(String.class).annotatedWith(Names.named(SSH_PATH_ENV)).toInstance(configProperties.getProperty(SSH_PATH_ENV));
          }
          
          bind(Properties.class).annotatedWith(Names.named(CONFIG_PROPERTIES_FILE)).toInstance(configProperties);
          
          //Bind Config file URL
          URL propsUrl = JChannelProvider.class.getClassLoader().getResource("tcp.xml");
          bind(URL.class).annotatedWith(Names.named(JChannelProvider.CONFIG_NAME)).toInstance(propsUrl);
          
          //Bind JChannel provider
          bind(JChannel.class).toProvider(JChannelProvider.class).in(Scopes.SINGLETON);
          
          //Bind CoordinatedWorker
          bind(CoordinatedWorker.class).to(CoordinatedWorkerImpl.class).in(Scopes.SINGLETON);          
          
          //Bind UpdateChannelReceiver
          bind(UpdateChannelReceiver.class).asEagerSingleton();
          
          //Bind Jersey UpdateService
          bind(UpdateService.class).in(Scopes.SINGLETON);
		}
      };
	}
}
