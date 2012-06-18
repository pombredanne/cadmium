package com.meltmedia.cadmium.core.git;

import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.meltmedia.cadmium.core.FileSystemManager;

public class LocalConfigSessionFactory extends JschConfigSessionFactory {
	
	private String privateKeyFile;
	private String knownHostsFile;
	private String sshDir;
	
	public LocalConfigSessionFactory( String sshDir ) {
		this.privateKeyFile = sshDir+"/meltmedia-gene-deploy";
		this.knownHostsFile = sshDir+"/known_hosts";
		this.sshDir = sshDir;
	}
	
	@Override
	protected void configure(Host host, Session session) {
		session.setUserInfo(new UserInfo() {

			@Override
			public String getPassphrase() {
				return new String(System.console().readPassword());
			}

			@Override
			public String getPassword() {
				return new String(System.console().readPassword());
			}

			@Override
			public boolean promptPassphrase(String arg0) {
        System.err.print("Enter "+arg0+": ");
				return true;
			}

			@Override
			public boolean promptPassword(String arg0) {
        System.err.print("Enter "+arg0+": ");
				return true;
			}

			@Override
			public boolean promptYesNo(String arg0) {
				return false;
			}

			@Override
			public void showMessage(String arg0) {
				System.err.println("Password or passphrase needed:");
			}
			
		});
	}

	
	@Override
	public synchronized RemoteSession getSession(URIish arg0,
			CredentialsProvider arg1, FS arg2, int arg3)
			throws TransportException {
		return super.getSession(arg0, arg1, arg2, arg3);
	}

	protected com.jcraft.jsch.JSch getJSch(OpenSshConfig.Host hc, FS fs)
      throws com.jcraft.jsch.JSchException
      {
		JSch jsch = super.getJSch(hc, fs);
    JSch.setConfig("StrictHostKeyChecking", "no");
		  if(FileSystemManager.exists(privateKeyFile)) {
		    jsch.addIdentity(privateKeyFile);
		  } else if (FileSystemManager.exists(sshDir + "/id_rsa")) {
		    jsch.addIdentity(sshDir + "/id_rsa");
		  }
	    jsch.setKnownHosts(knownHostsFile);
		return jsch;
      }

}