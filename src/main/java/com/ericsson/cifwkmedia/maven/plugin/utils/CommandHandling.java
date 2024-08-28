package com.ericsson.cifwkmedia.maven.plugin.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public class CommandHandling {

    public static void getStream(Process process, Log log) {
        StringBuffer output = new StringBuffer();
        String installData = null;
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String input = "";
        try {
            while ((input = stdInput.readLine()) != null) {
                installData += input + "\n";
            }
        } catch (IOException e) {
        }
        try {
            stdInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadArtifactToRepository(String parentPOMGAVandFileName, String latestMaven, String localNexusRepo, Log log){
        String [] projectGavArray = parentPOMGAVandFileName.split(":");
        String localGroupID = projectGavArray[0];
        String localArtifactID = projectGavArray[1];
        String localVersion= projectGavArray[2];
        String deployArtifactCommand =
                 latestMaven + " deploy:deploy-file -DgroupId=" + localGroupID
                 + " -DartifactId=" + localArtifactID
                 + " -Dversion=" + localVersion
                 + " -Dpackaging=pom -DgeneratePom=false" + " -Dfile="
                 + projectGavArray[3] + " -DrepositoryId=enm_iso_local"
                 + " -Durl=" + localNexusRepo + "\n";
        log.info("Command that will be run to upload  To Repository: " + deployArtifactCommand);
        executeCommand(deployArtifactCommand, log);
    }

    public static String executeCommand(String executeCommand, Log log) {
        Process process = null;
        String installData = "";
        try {
            process = Runtime.getRuntime().exec(executeCommand);
            ReadStream streamStdin = new ReadStream("stdin",
                    process.getInputStream(), log);
            ReadStream streamStderr = new ReadStream("stderr",
                    process.getErrorStream(), log);
            streamStdin.start();
            streamStderr.start();

            getStream(process, log);
            installData = streamStderr.getInstallData();

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                throw new MojoFailureException("Problem reading stream:");
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        } finally {
            if (process != null)
                process.destroy();
        }
        return installData;
    }
    
    public int getURLResponseCode(String inputURL, Log log){
        int responseCode = 0;
        try{
            URL url = new URL (inputURL);
            HttpURLConnection httpURLconnect =  ( HttpURLConnection )  url.openConnection (); 
            httpURLconnect.setRequestMethod ("GET");
            httpURLconnect.connect ();
            responseCode = httpURLconnect.getResponseCode();
        } catch (IOException error) {
            log.error("Error getting URL response code: " + error);
            error.printStackTrace();
        }
        return responseCode;
    }
}

