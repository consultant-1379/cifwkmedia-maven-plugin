package com.ericsson.cifwkmedia.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.ericsson.cifwkmedia.maven.plugin.utils.FileHandling;

/**
 * @goal buildMediaContents
 * @phase compile
 * @requiresProject false
 */
public class IdentifyMedia extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * @parameter property="useLatestApp" default-value="False"
     */
    @Parameter
    private String useLatestApp;

    /**
     * @parameter property="useLatestInfra" default-value="False"
     */
    @Parameter
    private String useLatestInfra;

    /**
     * @parameter property="useLatestPassedIso" default-value="False"
     */
    @Parameter
    private String useLatestPassedIso;

    /**
     * @parameter property="useLocalNexus" default-value="False"
     */
    @Parameter
    private String useLocalNexus;

    /**
     * @parameter property="baseIsoVersion" default-value="None"
     */
    @Parameter
    private String baseIsoVersion;

    /**
     * @parameter property="baseIsoName" default-value="None"
     */
    @Parameter
    private String baseIsoName;

    /**
     * @parameter property="cifwkGetDropContentsRestUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/getDropContents/"
     */
    @Parameter
    private String getDropContentsRestUrl;

    /**
     * @parameter property="getPreviousEventIDUrl" default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/getPreviousEventIDs/"
     */
    @Parameter
    private String getPreviousEventIDUrl;

    /**
     * @parameter property="cifwkGetLatestIsoUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/getlatestiso/"
     */
    @Parameter
    private String getLatestIsoUrl;

    /**
     * @parameter property="cifwkCompareMd5URL" default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/validateIsoUploadToHub/"
     */
    @Parameter
    private String cifwkCompareMd5URL;
    /**
     * @parameter property="media.os.version" default-value="LITP1"
     */
    @Parameter
    private String mediaOSVersion;

    /**
     * @parameter default-value="${localRepository}"
     */
    @Parameter
    private ArtifactRepository localRepository;

    /**
     * @parameter property="cifwkGetAOMRstateUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/getAOMRstate/"
     */
    @Parameter
    private String cifwkGetAOMRstateUrl;

    /**
     * Base directory of the project.
     *
     * @parameter default-value="${basedir}"
     */
    private String basedir;

    /**
     * @parameter property="media.category" default-value="product-iso"
     */
    @Parameter
    private String mediaCategory;

    /**
     * @parameter property="local.nexus.repo" default-value="https://arm901-eiffel004.athtem.eei.ericsson.se:8443/nexus/content/repositories/enm_iso_local"
     */
    @Parameter
    private String localNexusRepo;

    /**
     * @parameter property="hub.nexus.repo" default-value="https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/repositories/releases"
     */
    @Parameter
    private String nexusHUBURL;

    /**
     * @parameter property="only.include.media.catgeory.in.build" default-value="None"
     */
    @Parameter
    private String onlyIncludeMediaCategoryInBuild;

    /**
     * @parameter property="exclude.media.catgeory.from.build" default-value="None"
     */
    @Parameter
    private String excludeMediaCategory;

    /**
     * @parameter property="drop"
     *            default-value=""
     */
    @Parameter
    private String projectMediaDrop;

    private File isoDirectoryStructureConfigFile;
    private File rpmMediaCategoryMapping;
    private GetCIFWKDropContents getCIFWKDropContents = null;
    private String projectMediaProduct = "product";
    private String baseProjectDirNameProp = "base.dir.name";
    private String mediaContentName = "iso-dir";
    private String projectRstate = "ericsson.rstate";
    private String projectCXPNumber = "CXP";
    private String productBaseDirectory;
    private String projectMediaContentFile = "";
    private String deployArtifactCommandFile = "";
    private String isoContentEventFile;
    //private String mediaDrop = "";
    private String mediaProduct = "";
    private String baseProjectDirName = "";
    private Map<String, String> dropContentsMap = new HashMap<String, String>();
    private String getDropResult = "";
    private String localRepositoryName;
    private String latestMaven = "/home/lciadm100/tools/maven-latest/bin/mvn";
    private List<String> artifactGAVList = new ArrayList<String>();
    private String projectInjectedProperties;
    private String versionFile = "";
    private String versionContent;
    private boolean createDynamicallyCreatedISODirectories = false;
    private String errorMsg;

    public void execute() throws MojoExecutionException, MojoFailureException {

        localRepositoryName = localRepository.getUrl().replaceAll("file:", "");
        mediaProduct = project.getProperties().getProperty(projectMediaProduct);
        mediaContentName = project.getProperties().getProperty(mediaContentName);
        projectRstate = project.getProperties().getProperty(projectRstate);
        projectCXPNumber = project.getProperties().getProperty(projectCXPNumber);
        projectMediaContentFile = project.getBasedir() + "/target/mediaContent.txt";
        deployArtifactCommandFile = project.getBasedir() + "/target/deployArtifactCommand.txt";
        isoContentEventFile = project.getBasedir() + "/target/isoEvent.prop";
        baseProjectDirName = project.getProperties().getProperty(baseProjectDirNameProp);
        rpmMediaCategoryMapping = new File(basedir + "/" + "rpmMediaCategoryMapping.config");

        if (mediaCategory.contains("testware")) {
            isoDirectoryStructureConfigFile = new File(basedir + "/" + "testwareDirectoryMediaCategoryMapping.config");
        } else {
            isoDirectoryStructureConfigFile = new File(basedir + "/" + "directoryMediaCategoryMapping.config");
            if (!excludeMediaCategory.contains("None"))
            {
                excludeMediaCategory = excludeMediaCategory + ",testware";
            }else{
                excludeMediaCategory = "testware";
            }
        }
        if ( (!mediaCategory.contains("testware")) && ( onlyIncludeMediaCategoryInBuild != "None" ) ) {
            mediaCategory = onlyIncludeMediaCategoryInBuild;
            getLog().info("Media build will only contain: " + mediaCategory + " media category artifacts as selected in build properties.");
        }
        if (mediaOSVersion.contains("LITP1")) {
            if (project.getProperties().getProperty(baseProjectDirNameProp) != null) {
                productBaseDirectory = "/products/" + baseProjectDirName + "/" + projectCXPNumber + "_" + projectRstate + "/";
            } else {
                productBaseDirectory = "/products/" + mediaProduct + "/" + projectCXPNumber + "_" + projectRstate + "/";
            }
        }

        try {
            getLog().info("Retrieving Drop contents for Drop: " + projectMediaDrop + ", Product: " + mediaProduct);
            dropContentsMap.put("drop", projectMediaDrop);
            dropContentsMap.put("product", mediaProduct);
            dropContentsMap.put("baseIsoName", baseIsoName);
            dropContentsMap.put("baseIsoVersion", baseIsoVersion);
            dropContentsMap.put("useLatestInfra", useLatestInfra);
            dropContentsMap.put("useLatestApp", useLatestApp);
            dropContentsMap.put("useLatestPassedIso", useLatestPassedIso);
            dropContentsMap.put("useLocalNexus", useLocalNexus);
            if (mediaCategory != null && !mediaCategory.contains("None") && !mediaCategory.contains("product")) {
                dropContentsMap.put("mediaCategory", mediaCategory);
            }
            dropContentsMap.put("excludeMediaCategory", excludeMediaCategory);
            versionFile = project.getBasedir() + "/target/iso/.version";
            getCIFWKDropContents = new GetCIFWKDropContents();
            getDropResult = getCIFWKDropContents.getDropContents(dropContentsMap, getDropContentsRestUrl, getLatestIsoUrl, getLog());
        } catch (Exception error) {
            getLog().error("Error getting media information from cifwk" + error);
            try {
                throw new MojoFailureException("Error getting Media Data from cifwk DB :" + error);
            } catch (MojoFailureException error1) {
                getLog().error(error1);
                error1.printStackTrace();
            }
        }

        try {
            CreateLocalMediaContent createLocalMediaContent = new CreateLocalMediaContent();
            try {
                createDynamicallyCreatedISODirectories = createLocalMediaContent.createLocalDynamicMediaContentDirectories(isoDirectoryStructureConfigFile, mediaContentName, productBaseDirectory, mediaOSVersion, mediaCategory, excludeMediaCategory, getLog());
            } catch (Exception error) {
                getLog().error("Error setting ISO content property used for sending ISO content to Baseline Defined Event : " + error);
                try {
                    throw new MojoFailureException("Error setting ISO content property used for sending ISO content to Baseline Defined Event : " + error);
                } catch (MojoFailureException error1) {
                    getLog().error(error1);
                    error1.printStackTrace();
                }
            }
            if (createDynamicallyCreatedISODirectories) {
                try {
                    artifactGAVList = createLocalMediaContent.downLoadArtifactLocally(getDropResult, mediaContentName, mediaOSVersion, projectMediaContentFile, localRepositoryName, rpmMediaCategoryMapping, mavenSession, latestMaven, mediaCategory, localNexusRepo, getLog());
                } catch (MojoFailureException error) {
                    errorMsg = "Error downloading Artifacts Locally : " + error;
                    getLog().error(errorMsg);
                    throw new MojoFailureException(errorMsg);
                } catch (IOException IOError) {
                    errorMsg = "Error downloading Artifacts Locally : " + IOError;
                    getLog().error(errorMsg);
                    throw new MojoFailureException(errorMsg);
                }
            } else {
                errorMsg = "Failed to create ISO Directory Structure";
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);
            }

        } catch (Exception error) {
            errorMsg = "Error in creating local media content: " + error;
            getLog().error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }

        try {
            CreateLocalMediaContent createLocalMediaContent = new CreateLocalMediaContent();
            projectInjectedProperties = createLocalMediaContent.getISOContentJSON(artifactGAVList, project, getPreviousEventIDUrl, getLog());
        } catch (Exception error) {
            errorMsg = "Error setting ISO content property used for sending ISO content to Baseline Defined Event : " + error;
            getLog().error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }

        try {
            FileHandling fileHandling = new FileHandling();
            getLog().info("Gathering version information:");
            versionContent = fileHandling.getVersionInformation(project, mediaProduct, projectMediaDrop, cifwkGetAOMRstateUrl, getLog());
        } catch (Exception error) {
            getLog().error("Error getting the version information for the ISO : " + error);
            try {
                throw new MojoFailureException("Error getting the version information for the ISO: " + error);
            } catch (MojoFailureException error1) {
                getLog().error(error1);
                error1.printStackTrace();
            }
        }

        try {
            FileHandling.writeVersionInformation(versionContent, versionFile, getLog());
        } catch (Exception error) {
            getLog().error("Error creating ISO version file : " + error);
            try {
                throw new MojoFailureException("Error creating ISO version file : " + error);
            } catch (MojoFailureException error1) {
                getLog().error(error1);
                error1.printStackTrace();
            }
        }

        if (mediaCategory != null && !mediaCategory.contains("testware")) {
            try {
                getLog().info("Creating local ISO Event Property File:");
                FileHandling.writeDropContentsToLocalFile(projectInjectedProperties, isoContentEventFile, getLog());
            } catch (Exception error) {
                getLog().error("Error Creating local ISO Event Property File : " + error);
                try {
                    throw new MojoFailureException("Error Creating local ISO Event Property File : " + error);
                } catch (MojoFailureException error1) {
                    getLog().error(error1);
                    error1.printStackTrace();
                }
            }

            try {
                getLog().info("Creating Post ISO Build downstream Command File:");
                new FileHandling().createPostBuildContentsFile(project, localRepositoryName, latestMaven, nexusHUBURL, cifwkCompareMd5URL, deployArtifactCommandFile, getLog());
            } catch (Exception error) {
                getLog().error("Creating Temporary Deploy Media File: " + error);
                try {
                    throw new MojoFailureException("Creating Temporary Deploy Media File: " + error);
                } catch (MojoFailureException error1) {
                    getLog().error(error1);
                    error1.printStackTrace();
                }
            }
        }
    }
}
