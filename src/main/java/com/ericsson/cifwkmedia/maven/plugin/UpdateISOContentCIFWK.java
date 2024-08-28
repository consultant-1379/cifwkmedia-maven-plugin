package com.ericsson.cifwkmedia.maven.plugin;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @goal updateISOContentCIPortal
 * @phase deploy
 * @requiresProject false
 */
public class UpdateISOContentCIFWK extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter property="cifwkCreateISOContentRestUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/createISOContent/"
     */
    @Parameter
    private String updateISOContentRestURL;

    /**
     * @parameter property="createISOProductTestwareMappingResUrl"
     *            default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/createISOProductTestwareMapping/"
     */
    @Parameter
    private String createISOProductTestwareMappingResUrl;

    /**
     * @parameter property="media.category" default-value="product-iso"
     */
    @Parameter
    private String mediaCategory;
    
    /**
     * @parameter property="drop"
     *            default-value=""
     */
    @Parameter
    private String projectMediaDrop;

    private String projectMediaGroupIDKey = "group";
    private String projectMediaGroupIDValue = "";
    private String projectMediaVersionKey = "version";
    private String projectMediaVersionValue = "";
    private String projectMediaArtifactIDKey = "artifact";
    private String projectMediaArtifactIDValue = "";
    private String projectMediaDropKey = "drop";
    private String projectMediaProductKey = "product";
    private String projectMediaProductValue = "";
    private String projectMediaRepositoryKey = "repo";
    private String projectMediaRepositoryValue = "";
    private String projectMediaContentFile = "";
    private String mediaArtifactNameParameter = "";
    private String mediaArtifactVersionParameter = "";
    private List<BasicNameValuePair> ISOContentsMap = new ArrayList<BasicNameValuePair>();
    private List<BasicNameValuePair> ISOProductTestwareMap = new ArrayList<BasicNameValuePair>();
    public String getDropResult = "";
    public String isoContents = "";

    public void execute() throws MojoExecutionException, MojoFailureException {

        projectMediaVersionValue = project.getVersion();

        if (projectMediaVersionValue.contains("SNAPSHOT")) {
            projectMediaVersionValue = projectMediaVersionValue.replace("-SNAPSHOT", "");
        }

        projectMediaGroupIDValue = project.getGroupId();
        projectMediaArtifactIDValue = project.getArtifactId();
        projectMediaProductValue = project.getProperties().getProperty(projectMediaProductKey);
        projectMediaRepositoryValue = project.getProperties().getProperty("release.repo");
        projectMediaContentFile = project.getBasedir() + "/target/mediaContent.txt";

        try {
            isoContents = readFile(projectMediaContentFile, StandardCharsets.UTF_8);
        } catch (IOException IOerror) {
            getLog().error("Error creating project Media Contents File :" + IOerror);
            throw new MojoFailureException("Error creating project Media Contents File :" + IOerror);

        }

        getLog().info("*** Adding The following package Info to the database ***");
        getLog().info("GroupID :" + projectMediaGroupIDValue);
        getLog().info("Artifact ID :" + projectMediaArtifactIDValue);
        getLog().info("Version :" + projectMediaVersionValue);
        getLog().info("Drop :" + projectMediaDrop);
        getLog().info("Product :" + projectMediaProductValue);
        getLog().info("Repository :" + projectMediaRepositoryValue);

        getLog().debug("ISO Contents :" + isoContents);
        ISOContentsMap.add(new BasicNameValuePair(projectMediaGroupIDKey, projectMediaGroupIDValue));
        ISOContentsMap.add(new BasicNameValuePair(projectMediaArtifactIDKey, projectMediaArtifactIDValue));
        ISOContentsMap.add(new BasicNameValuePair(projectMediaVersionKey, projectMediaVersionValue));
        ISOContentsMap.add(new BasicNameValuePair(projectMediaDropKey, projectMediaDrop));
        ISOContentsMap.add(new BasicNameValuePair(projectMediaProductKey, projectMediaProductValue));
        ISOContentsMap.add(new BasicNameValuePair(projectMediaRepositoryKey, projectMediaRepositoryValue));
        ISOContentsMap.add(new BasicNameValuePair("content", isoContents));

        try {
            new GenericRestCalls().setUpPOSTRestCall(ISOContentsMap, updateISOContentRestURL, getLog());
        } catch (Exception error) {
            getLog().error("Error posting media information from cifwk" + error);
            throw new MojoFailureException("Error posting Media Data from cifwk DB :" + error);
        }

        if (!mediaCategory.contains("testware")) {
            mediaArtifactNameParameter = "productISOArtifactName";
            mediaArtifactVersionParameter = "productISOVersion";
        } else {
            mediaArtifactNameParameter = "testwareISOArtifactName";
            mediaArtifactVersionParameter = "testwareISOVersion";
        }

        getLog().info("*** Adding The following Media Artifact Mapping Info to the database ***");
        getLog().info("Drop :" + projectMediaDrop);
        getLog().info("Product :" + projectMediaProductValue);
        getLog().info("Media Artifact Name :" + projectMediaArtifactIDValue);
        getLog().info("Media Artifact Version :" + projectMediaVersionValue);

        ISOProductTestwareMap.add(new BasicNameValuePair(projectMediaDropKey, projectMediaDrop));
        ISOProductTestwareMap.add(new BasicNameValuePair(projectMediaProductKey, projectMediaProductValue));
        ISOProductTestwareMap.add(new BasicNameValuePair(mediaArtifactNameParameter, projectMediaArtifactIDValue));
        ISOProductTestwareMap.add(new BasicNameValuePair(mediaArtifactVersionParameter, projectMediaVersionValue));

        try {
            new GenericRestCalls().setUpPOSTRestCall(ISOProductTestwareMap, createISOProductTestwareMappingResUrl, getLog());
        } catch (MojoFailureException error) {
            getLog().error("Error posting Media Artifact Mapping Info to the database" + error);
            throw new MojoFailureException("Error posting Media Artifact Mapping Info to the database :" + error);
        }
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
