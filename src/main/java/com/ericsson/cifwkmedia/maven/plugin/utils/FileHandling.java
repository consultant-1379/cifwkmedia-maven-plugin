package com.ericsson.cifwkmedia.maven.plugin.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.ericsson.cifwkmedia.maven.plugin.GenericRestCalls;

public class FileHandling {
    public static boolean createMediaDirectory = true;
    private String localArtifactLocation;
    private String deployArtifactCommand;
    private String localArtifact;
    private String groupId;
    private String artifactId;
    private String version;
    private String versionInfo = "";
    private String header = "";
    private List<NameValuePair> versionInfoMap = new ArrayList<NameValuePair>();

    public static boolean createISODirectory(String directory, Log log){
        try {
            File file = new File(directory);
            createMediaDirectory = deleteDir(file, log);
            if (createMediaDirectory == false) {
                throw new MojoFailureException("Issue with Deleting Existing Directory and Contents for: "
                        + directory);
            }
            log.info("Creating local media directory: " + directory);
            file.mkdirs();
        } catch (Exception error) {
            log.error("Error: in creating local directory for Media Artifacts: "
                    + error);
            try {
                throw new MojoFailureException("Error: in creating local directory for Media Artifacts :"
                        + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void createPostBuildContentsFile(MavenProject project, String localRepositoryName, String latestMaven, String nexusHUBURL, String cifwkCompareMd5URL, String deployArtifactCommandFile, Log log){
        groupId = project.getGroupId();
        artifactId = project.getArtifactId();
        version = project.getVersion();
        localArtifact = project.getArtifactId() + "-" + project.getVersion() + ".iso";
        localArtifactLocation = localRepositoryName
                + groupId.replaceAll("\\.", "/") + "/"
                + artifactId + "/" + version
                + "/" + localArtifact;
        deployArtifactCommand = "/bin/mv " + localArtifactLocation + " " + localRepositoryName + "\n";

        deployArtifactCommand = deployArtifactCommand
                + latestMaven + " deploy:deploy-file -DgroupId=" + groupId
                + " -DartifactId=" + artifactId
                + " -Dversion=" + version
                + " -Dfile=" + localRepositoryName + localArtifact
                + " -DrepositoryId=releases"
                + " -Durl=" + nexusHUBURL + "\n";

        deployArtifactCommand = deployArtifactCommand + "/bin/rm -rf " + localRepositoryName + localArtifact + "\n";

        deployArtifactCommand = deployArtifactCommand
                + "curl -k --url \""
                + cifwkCompareMd5URL + "?group="
                + groupId + "&artifact=" + artifactId
                + "&version=" + version
                + "&extension=iso\"\n";
        writeDropContentsToLocalFile(deployArtifactCommand, deployArtifactCommandFile, log);
    }

    public String getVersionInformation(MavenProject project, String mediaProduct, String mediaDrop, String restUrl, Log log) {
        versionInfoMap.add(new BasicNameValuePair("product", mediaProduct));
        versionInfoMap.add(new BasicNameValuePair("drop", mediaDrop));
        try {
            header = mediaProduct + " " + mediaDrop + " (ISO Version: " + project.getVersion() + ") ";
            versionInfo =  header + new GenericRestCalls().setUpGETRestCall(versionInfoMap, restUrl, log);
        } catch (Exception error) {
            log.error("Error in getting http response from generic rest call getAOMRstate: "
                    + error);
            try {
                throw new MojoFailureException(
                        "Error in getting http response from generic rest call getAOMRstate: "
                                + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        return versionInfo;
    }

    public static void writeVersionInformation(String content, String versionFile, Log log) {
        try {
            File vFile = new File(versionFile);
            if (vFile.isFile()) {
                log.info(versionFile
                        + " File already existings locally, deleting: "
                        + vFile);
                vFile.delete();
            }
            log.info("Creating version file: " + vFile);
            vFile.createNewFile();

            log.info("Writing Version Information to version file: " + vFile);
            FileWriter fw = new FileWriter(vFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

        } catch (IOException error) {
            error.printStackTrace();
            try {
                throw new MojoFailureException("Error writing version information to file :"
                        + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
    }

    public static boolean deleteDir(File dir, Log log) {
        if (dir.isDirectory()) {
            log.info("Directory Exists Deleting contents and Main Directory: "
                    + dir);
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]), log);
                if (!success) {
                    return false;
                }
            }
            dir.delete();
        }
        return true;
    }

    public static void writeDropContentsToLocalFile(String result, String contentsFile, Log log) {
        try {
            File contentfile = new File(contentsFile);
            if (contentfile.isFile()) {
                log.info(contentsFile
                        + " File already existings locally, deleting: "
                        + contentfile);
                contentfile.delete();
            }
            log.info("Creating Content local file: " + contentfile);
            contentfile.createNewFile();

            log.info("Writing Content to local file: " + contentfile);
            FileWriter fw = new FileWriter(contentfile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(result);
            bw.close();

        } catch (IOException error) {
            error.printStackTrace();
            try {
                throw new MojoFailureException("Error writing contents to File :"
                        + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
    }

    public static String copyFindReplaceInFile(File fileName, String tempFileName, Map<String, String> details) {

        Path tempFilePath = null;
        File tempFile = null;
        try {
            tempFile = File.createTempFile(tempFileName, ".xml");
            tempFilePath = Paths.get(tempFile.getAbsolutePath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Charset charset = StandardCharsets.UTF_8;
        String content;

        try {
            copyContentsOfFile(fileName, tempFile);
            content = new String(Files.readAllBytes(tempFilePath), charset);
            Iterator<Entry<String, String>> iterator = details.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, String> next = iterator.next();
                Map.Entry<String, String> pairs = (Map.Entry<String, String>) next;
                content = content.replace(pairs.getKey().toString(), pairs.getValue().toString());
                iterator.remove();
                Files.write(tempFilePath, content.getBytes(charset));
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }

    public static void copyContentsOfFile(File fin, File dest) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fin);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        FileWriter fstream = null;
        try {
            fstream = new FileWriter(dest.getAbsolutePath(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter out = new BufferedWriter(fstream);

        String aLine = null;
        try {
            while ((aLine = in.readLine()) != null) {
                out.write(aLine);
                out.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
