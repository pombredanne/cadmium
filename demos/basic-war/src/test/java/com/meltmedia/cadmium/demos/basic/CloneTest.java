package com.meltmedia.cadmium.demos.basic;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.junit.Ignore;
import org.junit.Test;

import com.meltmedia.cadmium.jgit.impl.GithubConfigSessionFactory;

@Ignore
public class CloneTest {

	@Test
	public void test() throws Exception {

		String applicationBasePath = "/Library/WebServer/Test";
		String repoDir = applicationBasePath+"/git-checkout";
		File repoFile = new File(repoDir);
		try {
		SshSessionFactory.setInstance(new GithubConfigSessionFactory(applicationBasePath+"/.ssh"));
		CloneCommand cloneCommand = Git.cloneRepository()
        .setBare(false)
		.setURI("git@github.com:meltmedia/cadmium-static-content-example.git")
		.setBranch("HEAD")
		.setDirectory(repoFile)
		.setTimeout(60);
		cloneCommand.call();
		}
		catch( Exception e ) {
			repoFile.delete();
			throw e;
		}
	}

}
