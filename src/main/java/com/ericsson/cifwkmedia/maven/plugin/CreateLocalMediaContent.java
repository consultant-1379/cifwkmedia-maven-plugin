package com.ericsson.cifwkmedia.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.ericsson.cifwkmedia.maven.plugin.utils.CommandHandling;
import com.ericsson.cifwkmedia.maven.plugin.utils.FileHandling;

public class CreateLocalMediaContent {

    public String mediaApplictaionDirectory = "";
    public String mediaThirdPartyProductsDirectory = "";
    public String mediaPluginsDirectory = "";
    public String mediaImagesDirectory = "";
    public String mediaModelsDirectory = "";
    public String allPackagesDirectory = "";
    private Map<String, String> artifactDetails = new HashMap<String, String>();
    private String groupId;
    private String artifactId;
    private String version = artifactDetails.get("version");
    private String packageURL;
    private String m2Type;
    private String baseName;
    private File mediafile;
    private File localStoredArtifact;
    private File localStoredMD5Artifact;
    private String localArtifactName;
    private File localStoredArtifactDir;
    private File localStoredMD5ArtifactDir;
    private List<String> artifactGAVList = new ArrayList<String>();
    private static String ISOContentJSON = "ISOBUILD_BUILDCONTENT";
    private String projectInjectedProperties = "";
    private List<NameValuePair> previousEventMap = new ArrayList<NameValuePair>();
    public Map<String, String> isoDirStructureCategoryMap = new HashMap<String, String>();
    public Map<String, String> rpmMediaCatMapping = new HashMap<String, String>();
    public String mediaDirectory;
    public List<String> artifactList = new ArrayList<String>();
    public List<String> mediaCategoryList = new ArrayList<String>();
    public String projectRootDirectory = "";
    private File templateFile;
    private Map<String, String> dependencyDetails = new HashMap<String, String>();
    public String mediaCategoryMediaDirectoryMap;
    private String mediaCategory = "";
    private String errorMsg;

    public boolean createLocalDynamicMediaContentDirectories(File isoDirectoryStructureConfigFile, String mediaContentName, String productBaseDirectory, String mediaOSVersion, String dropMediaCategoryType, String excludeMediaCategory, Log log) throws IOException {
        LineIterator iterateDirMediaCatMapping = FileUtils.lineIterator(isoDirectoryStructureConfigFile, "UTF-8");
        try {
            while (iterateDirMediaCatMapping.hasNext()) {
                String line = iterateDirMediaCatMapping.nextLine();
                if ((!line.startsWith("#") && (!line.isEmpty()))) {
                    String[] categoryDirectoryKeyValue = line.split("::");
                    if (! excludeMediaCategory.contains(categoryDirectoryKeyValue[1])){
                        isoDirStructureCategoryMap.put(categoryDirectoryKeyValue[1], categoryDirectoryKeyValue[0] + "/");
                        FileHandling.createISODirectory(mediaContentName + categoryDirectoryKeyValue[0], log);
                    }else{
                        log.info("Directory: " + categoryDirectoryKeyValue[0] + " with Media Category: " + categoryDirectoryKeyValue[1] + " will not be created as it was choosen to exclude this media category in build properties.");
                    }
                    mediaCategoryList.add(categoryDirectoryKeyValue[1]);
                }
            }
        } finally {
            iterateDirMediaCatMapping.close();
        }

        if (mediaOSVersion.contains("LITP1") && dropMediaCategoryType != null && !dropMediaCategoryType.contains("testware")) {
            allPackagesDirectory = productBaseDirectory;
            FileHandling.createISODirectory(mediaContentName + allPackagesDirectory, log);
        }
        return true;
    }

    public List<String> downLoadArtifactLocally(String result, String mediaContentName, String mediaOSVersion, String projectMediaContentFile, String localRepositoryName, File rpmMediaCategoryMapping, MavenSession session, String latestMaven, String dropMediaCategoryType, String localNexusRepo, Log log) throws IOException, MojoFailureException {
        if (rpmMediaCategoryMapping.exists()) {
            LineIterator iteraterpmMediaCatMapping = FileUtils.lineIterator(rpmMediaCategoryMapping, "UTF-8");
            try {
                while (iteraterpmMediaCatMapping.hasNext()) {
                    String line = iteraterpmMediaCatMapping.nextLine().trim();
                    if ((!line.startsWith("#") && (!line.isEmpty()))) {
                        String[] rpmMediaCategoryKeyValue = line.split("::");
                        rpmMediaCatMapping.put(rpmMediaCategoryKeyValue[0], rpmMediaCategoryKeyValue[1]);
                        artifactList.add(rpmMediaCategoryKeyValue[0].toString());
                    }
                }
            } finally {
                iteraterpmMediaCatMapping.close();
            }
        }
        FileHandling.writeDropContentsToLocalFile(result, projectMediaContentFile, log);
        JSONArray jsonArray = new JSONArray(result);
        for (int i = 0; i < jsonArray.length(); i++) {
            String jsonReturnArtifactName = jsonArray.getJSONObject(i).getString("name");
            String pomMediaCategory = jsonArray.getJSONObject(i).getString("mediaCategory");
            String packageURL = jsonArray.getJSONObject(i).getString("url");
            String localGroupId = jsonArray.getJSONObject(i).getString("group");
            String localVersion = jsonArray.getJSONObject(i).getString("version");
            projectRootDirectory = session.getExecutionRootDirectory();
            artifactDetails.put("packageURL", packageURL);
            artifactDetails.put("groupId", localGroupId);
            artifactDetails.put("artifactId", jsonReturnArtifactName);
            artifactDetails.put("version", localVersion);
            artifactDetails.put("localRepo", localRepositoryName);

            if (pomMediaCategory.contains("testware")) {
                mediaCategory = pomMediaCategory;
                artifactGAVList = handleProjectDefinedMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                continue;
            }

            String[] mediaCategoryTypes = pomMediaCategory.split(",");
            if (!mediaCategoryTypes.equals(null)) {
                for (String mediaCategoryType : mediaCategoryTypes) {
                    mediaCategory = mediaCategoryType;
                    if (mediaCategoryList.contains(mediaCategory)) {
                        artifactGAVList = handleProjectDefinedMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                        continue;
                    } else {
                        if (!artifactList.isEmpty()) {
                            if (artifactList.contains(jsonReturnArtifactName)) {
                                artifactGAVList = handleMediaCategoryManifestEntry(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                            } else {
                                artifactGAVList = handleDefaultMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                                continue;
                            }
                        } else {
                            artifactGAVList = handleDefaultMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                            continue;
                        }
                    }
                }
            } else {
                if (!artifactList.isEmpty()) {
                    if (artifactList.contains(jsonReturnArtifactName)) {
                        artifactGAVList = handleMediaCategoryManifestEntry(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                    } else {
                        artifactGAVList = handleDefaultMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                        continue;
                    }
                } else {
                    artifactGAVList = handleDefaultMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                    continue;
                }
            }

        }
        return artifactGAVList;
    }

    public void getPackage(Map<String, String> artifactDetails, String directory, String mediaOSVersion, String mediaCategory, String mediaContentName, String projectRootDirectory, String dropMediaCategoryType, Log log) throws IOException, MojoFailureException {
        groupId = artifactDetails.get("groupId");
        artifactId = artifactDetails.get("artifactId");
        version = artifactDetails.get("version");
        packageURL = artifactDetails.get("packageURL");
        m2Type = FilenameUtils.getExtension(packageURL);
        baseName = FilenameUtils.getBaseName(packageURL);
        localArtifactName = artifactDetails.get("localRepo") + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "." + m2Type;
        mediafile = new File(directory + baseName + "." + m2Type);
        localStoredArtifact = new File(localArtifactName);
        localStoredArtifactDir = new File(FilenameUtils.getFullPathNoEndSeparator(localStoredArtifact.getAbsolutePath()));

        try {
            if (!localStoredArtifact.exists()) {
                log.debug(localStoredArtifact + " does not exist therefore downloading " + baseName + "." + m2Type + " to: " + directory);
                FileUtils.copyURLToFile(new URL(packageURL), mediafile);
                log.debug(baseName + " does not exists in local Maven Repository, therefore copying: " + mediafile + " to local Repository: " + localArtifactName);
                try {
                    if (!localStoredArtifactDir.exists()) {
                        FileUtils.forceMkdir(localStoredArtifactDir);
                    }
                    log.debug("Copying " + mediafile + " to: " + localStoredArtifactDir);
                    FileUtils.copyFileToDirectory(mediafile, localStoredArtifactDir);

                } catch (Exception error) {
                    errorMsg = "Error copying : " + mediafile + " to local Repository: " + localArtifactName + " Error: " + error;
                    log.error(errorMsg);
                    throw new MojoFailureException(errorMsg);
                }
            } else {
                try {
                    log.debug("Copying " + localStoredArtifact + " to: " + mediafile);
                    FileUtils.copyFile(localStoredArtifact, mediafile);
                } catch (Exception error) {
                    errorMsg = "Error copying : " + localStoredArtifact + " to: " + mediafile + " Error: " + error;
                    log.error(errorMsg);
                    throw new MojoFailureException(errorMsg);
                }
            }
            if (directory.contains("image")) {
                String md5LocalArtifactName = localArtifactName + ".md5";
                localStoredMD5Artifact = new File(md5LocalArtifactName);
                localStoredMD5ArtifactDir = new File(FilenameUtils.getFullPathNoEndSeparator(localStoredMD5Artifact.getAbsolutePath()));
                File md5Mediafile = new File(directory + baseName + "." + m2Type + ".md5");
                if (!localStoredMD5Artifact.exists()) {
                    log.debug(md5LocalArtifactName + " does not exist therefore downloading " + packageURL + ".md5" + " to: " + md5Mediafile);
                    FileUtils.copyURLToFile(new URL(packageURL + ".md5"), md5Mediafile);
                    log.debug("Copying " + md5Mediafile + " to: " + localStoredMD5ArtifactDir);
                    FileUtils.copyFileToDirectory(md5Mediafile, localStoredMD5ArtifactDir);
                } else {
                    try {
                        log.debug("Copying " + localStoredMD5Artifact + " to: " + md5Mediafile);
                        FileUtils.copyFile(localStoredMD5Artifact, md5Mediafile);
                    } catch (Exception error) {
                        errorMsg = "Error copying : " + localStoredMD5Artifact + " to: " + md5Mediafile + " Error: " + error;
                        log.error(errorMsg);
                        throw new MojoFailureException(errorMsg);
                    }
                }
            }
            if (mediaOSVersion.contains("LITP1") && mediaCategory == null) {
                File mediafileProducts = new File(mediaContentName + allPackagesDirectory);
                log.debug("Copying " + mediafile + " to " + mediafileProducts + " as LITP1 build.");
                FileUtils.copyFileToDirectory(mediafile, mediafileProducts);
            }
        } catch (IOException IOError) {
            errorMsg = "Error in get Package Function: " + IOError;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }

    public String getISOContentJSON(List<String> artifactGAVList, MavenProject project, String getPreviousEventIDUrl, Log log) throws MojoFailureException {
        try {
            List<String> artifactList = new ArrayList<String>();
            JSONObject artifactGAVObj = new JSONObject();
            JSONObject mainObj = new JSONObject();
            for (String artifactGAV : artifactGAVList) {
                String[] GAVList = artifactGAV.split(":");
                artifactGAVObj.put("groupId", GAVList[1]);
                artifactGAVObj.put("artifactId", GAVList[2]);
                artifactGAVObj.put("version", GAVList[3]);
                mainObj.put("gav", artifactGAVObj);
                mainObj.put("tag", GAVList[0]);
                artifactList.add(mainObj.toString());
            }
            projectInjectedProperties = ISOContentJSON + "=" + artifactList.toString() + "\n";
            projectInjectedProperties = projectInjectedProperties + "ISOBUILD_POM_GROUPID=" + project.getGroupId() + "\n" + "ISOBUILD_POM_ARTIFACTID=" + project.getArtifactId() + "\n" + "ISOBUILD_POM_VERSION=" + project.getVersion() + "\n";
        } catch (Exception error) {
            errorMsg = "Error building image content JSON: " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        } finally {
            artifactGAVList.clear();
        }
        previousEventMap.add(new BasicNameValuePair("groupId", project.getGroupId()));
        previousEventMap.add(new BasicNameValuePair("artifactId", project.getArtifactId()));
        previousEventMap.add(new BasicNameValuePair("version", project.getVersion()));
        try {
            projectInjectedProperties = projectInjectedProperties + new GenericRestCalls().setUpGETRestCall(previousEventMap, getPreviousEventIDUrl, log);
        } catch (Exception error) {
            errorMsg = "Error in getting http response from generic rest call getLatestIsoUrl: " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
        return projectInjectedProperties;
    }

    public List<String> handleDefaultMediaCategory(String jsonReturnArtifactName, String mediaContentName, String mediaOSVersion, String dropMediaCategoryType, Log log) throws MojoFailureException {
        try {
            log.info("Media Category in local project: '" + jsonReturnArtifactName + "' POM is set to: '" + mediaCategory + "'");
            mediaCategory = "service";
            log.warn("Media Category in local project: '" + jsonReturnArtifactName + "' is not a valid Media Category, therefore the default Category: '" + mediaCategory + "', will be used.");
            artifactGAVList = handleProjectDefinedMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
            return artifactGAVList;
        } catch (Exception error) {
            errorMsg = "Error: Handling default Media Category. " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }

    public List<String> handleMediaCategoryManifestEntry(String jsonReturnArtifactName, String mediaContentName, String mediaOSVersion, String dropMediaCategoryType, Log log) throws MojoFailureException {
        try {
            log.info("Media Category in local project: '"+ jsonReturnArtifactName + "' POM is set to: '" + mediaCategory + "'");
            if (artifactList.contains(jsonReturnArtifactName)) {
                String artifactMediaCategories = (String) rpmMediaCatMapping.get(jsonReturnArtifactName);
                String[] artifactMediaCategoryTypes = artifactMediaCategories.split(",");
                for (String mediaCategoryType : artifactMediaCategoryTypes) {
                    mediaCategory = mediaCategoryType;
                    log.warn("Media Category in local project: '" + jsonReturnArtifactName + "' POM is a not a valid Media Category type therefore '" + mediaCategory + "', will be used.");
                    artifactGAVList = handleProjectDefinedMediaCategory(jsonReturnArtifactName, mediaContentName, mediaOSVersion, dropMediaCategoryType, log);
                    continue;
                }
            }
            return artifactGAVList;
        } catch (Exception error) {
            errorMsg = "Error: with handling Media Category in ManifestEntry. " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }

    public List<String> handleProjectDefinedMediaCategory(String jsonReturnArtifactName, String mediaContentName, String mediaOSVersion, String dropMediaCategoryType, Log log) throws MojoFailureException {
        try {
            artifactGAVList.add(mediaCategory + ":" + groupId + ":" + jsonReturnArtifactName + ":" + version);
            mediaCategoryMediaDirectoryMap = (String) isoDirStructureCategoryMap.get(mediaCategory);
            mediaDirectory = mediaContentName + mediaCategoryMediaDirectoryMap;
            if (mediaCategoryMediaDirectoryMap != null) {
                if (mediaCategory.equalsIgnoreCase("image")) {
                    getPackage(artifactDetails, mediaDirectory, mediaOSVersion, mediaCategory, mediaContentName, projectRootDirectory, dropMediaCategoryType, log);
                } else {
                    getPackage(artifactDetails, mediaDirectory, mediaOSVersion, null, mediaContentName, projectRootDirectory, dropMediaCategoryType, log);
                }
            }
            return artifactGAVList;
        } catch (Exception error) {
            errorMsg = "Error: with handling Media Category with valid project declared type. " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }
    }
}
