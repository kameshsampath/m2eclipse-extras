/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.pomproperties.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * Generates pom.properties and pom.xml files under META-INF/maven/${project.groupId}/${project.artifactId} folder during
 * eclipse build. <br/>
 * In maven, this behaviour is implemented by org.apache.maven.archiver.PomPropertiesUtil from org.apache.maven
 * maven-archiver. Therefore, all mojos that use MavenArchiver (jar mojo, ejb mojo and so on) produce this file during
 * cli build.
 * 
 * @see https://svn.apache.org/repos/asf/maven/shared/trunk/maven-archiver/src/main/java/org/apache/maven/archiver/
 *      PomPropertiesUtil.java
 * @see http://maven.apache.org/shared/maven-archiver/index.html
 * @author igor
 */
public class PomPropertiesConfigurator extends AbstractProjectConfigurator {

  private static final String GENERATED_BY_M2E = "Generated by m2e";

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    //nothing to configure
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
                                                      IPluginExecutionMetadata executionMetadata) {
    return new AbstractBuildParticipant() {
      public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        writePom(getMavenProjectFacade(), monitor);
        return null;
      }
    };
  }

  void writePom(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    IProject project = facade.getProject();
    ArtifactKey mavenProject = facade.getArtifactKey();
    IWorkspaceRoot root = project.getWorkspace().getRoot();

    IPath outputPath = facade.getOutputLocation().append("META-INF/maven").append(mavenProject.getGroupId()).append(
        mavenProject.getArtifactId());

    IFolder output = root.getFolder(outputPath);
    M2EUtils.createFolder(output, true);

    Properties properties = new Properties();
    properties.put("groupId", mavenProject.getGroupId());
    properties.put("artifactId", mavenProject.getArtifactId());
    properties.put("version", mavenProject.getVersion());
    properties.put("m2e.projectName", project.getName());
    properties.put("m2e.projectLocation", project.getLocation().toOSString());

    IFile pomProperties = output.getFile("pom.properties");
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try {
      properties.store(buf, GENERATED_BY_M2E);
    } catch(IOException ex) {
    }

    if(pomProperties.exists()) {
      pomProperties.setContents(new ByteArrayInputStream(buf.toByteArray()), IResource.FORCE, monitor);
    } else {
      pomProperties.create(new ByteArrayInputStream(buf.toByteArray()), IResource.FORCE, monitor);
    }

    IFile pom = output.getFile("pom.xml");
    InputStream is = facade.getPom().getContents();
    try {
      if(pom.exists()) {
        pom.setContents(is, IResource.FORCE, monitor);
      } else {
        pom.create(is, IResource.FORCE, monitor);
      }
    } finally {
      IOUtil.close(is);
    }
  }

}
