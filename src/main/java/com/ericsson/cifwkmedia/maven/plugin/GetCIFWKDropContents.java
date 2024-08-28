package com.ericsson.cifwkmedia.maven.plugin;

import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.JSONArray;

public class GetCIFWKDropContents {

    private final String projectMediaDrop = "drop";
    private final String projectMediaProduct = "product";
    private final String baseIsoName = "baseIsoName";
    private final String baseIsoVersion = "baseIsoVersion";
    private final String useLatestPassedIso = "useLatestPassedIso";
    private final String useLatestInfra = "useLatestInfra";
    private final String useLatestApp = "useLatestApp";
    private final String useLocalNexus = "useLocalNexus";
    private final String mediaCategory = "mediaCategory";
    private final String excludeMediaCategory = "excludeMediaCategory";

    private GenericRestCalls genericRestCall = null;
    public String result = "";

    public String getDropContents(Map<String, String> dropContentsMap, String getDropContentsRestUrl, String getLatestIsoUrl, Log log) throws MojoExecutionException, MojoFailureException {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (dropContentsMap.get(useLatestPassedIso).equalsIgnoreCase("true")) {
            String latestIsoJson = getLatestIso(dropContentsMap, getLatestIsoUrl, log);
            JSONArray jsonArray = new JSONArray(latestIsoJson);
            String latestIsoName = jsonArray.getJSONObject(0).getString("isoName");
            String latestIsoVersion = jsonArray.getJSONObject(0).getString("isoVersion");
            dropContentsMap.put(baseIsoName, latestIsoName);
            dropContentsMap.put(baseIsoVersion, latestIsoVersion);
        }
        log.info("Get Drop Contents Rest Call run on: " + getDropContentsRestUrl);
        log.info("*** Getting Drop Contents for ***");
        log.info("Media Drop :" + dropContentsMap.get(projectMediaDrop));
        log.info("Media Product :" + dropContentsMap.get(projectMediaProduct));
        log.info("Base ISO Name :" + dropContentsMap.get(baseIsoName));
        log.info("Base ISO Version :" + dropContentsMap.get(baseIsoVersion));
        log.info("Exclude Content Media Category :" + dropContentsMap.get(excludeMediaCategory));
        log.info("Use latest Passed ISO:" + dropContentsMap.get(useLatestPassedIso));
        log.info("Use latest Infra :" + dropContentsMap.get(useLatestInfra));
        log.info("Use latest App :" + dropContentsMap.get(useLatestApp));
        log.info("Use local Nexus :" + dropContentsMap.get(useLocalNexus));

        params.add(new BasicNameValuePair(projectMediaDrop, dropContentsMap.get(projectMediaDrop)));
        params.add(new BasicNameValuePair(projectMediaProduct, dropContentsMap.get(projectMediaProduct)));
        if (dropContentsMap.get(useLatestInfra).equalsIgnoreCase("true")) {
            params.add(new BasicNameValuePair("useLatestInfra", "True"));
        }
        if (dropContentsMap.get(useLatestApp).equalsIgnoreCase("true")) {
            params.add(new BasicNameValuePair("useLatestApp", "True"));
        }
        if (dropContentsMap.get(useLocalNexus).equalsIgnoreCase("true")) {
            params.add(new BasicNameValuePair("useLocalNexus", "True"));
        }
        if (dropContentsMap.get(baseIsoName) != "None") {
            params.add(new BasicNameValuePair("baseIsoName", dropContentsMap.get(baseIsoName)));
        }
        if (dropContentsMap.get(baseIsoVersion) != "None") {
            params.add(new BasicNameValuePair("baseIsoVersion", dropContentsMap.get(baseIsoVersion)));
        }
        if (dropContentsMap.get(mediaCategory) != null) {
            params.add(new BasicNameValuePair("mediaCategory", dropContentsMap.get(mediaCategory)));
        }
        if (dropContentsMap.get(excludeMediaCategory) != null) {
            params.add(new BasicNameValuePair("excludeMediaCategory", dropContentsMap.get(excludeMediaCategory)));
        }

        String ErrorMsg = "Error in getting http response from generic rest call set ip Get rest call.";
        try {
            genericRestCall = new GenericRestCalls();
            result = genericRestCall.setUpGETRestCall(params, getDropContentsRestUrl, log);
            if (result.contains("error")) {
                log.error(ErrorMsg);
                throw new MojoFailureException(ErrorMsg);
            }
        } catch (Exception error) {
            log.error(ErrorMsg + ":" + error);
            try {
                throw new MojoFailureException(ErrorMsg + ":" + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        return result;

    }

    public String getLatestIso(Map<String, String> dropContentsMap, String getLatestIsoUrl, Log log) throws MojoExecutionException, MojoFailureException {
        log.info("Get latest ISO Rest Call run on: " + getLatestIsoUrl);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("passedOnly", "True"));
        params.add(new BasicNameValuePair(projectMediaDrop, dropContentsMap.get(projectMediaDrop)));
        params.add(new BasicNameValuePair(projectMediaProduct, dropContentsMap.get(projectMediaProduct)));
        String ErrorMsg = "Error in getting http response from generic rest call set ip Get rest call.";
        try {
            genericRestCall = new GenericRestCalls();
            result = genericRestCall.setUpGETRestCall(params, getLatestIsoUrl, log);
            if (result.contains("error")) {
                log.error(ErrorMsg);
                throw new MojoFailureException(ErrorMsg);
            }
        } catch (Exception error) {
            log.error(ErrorMsg + ":" + error);
            try {
                throw new MojoFailureException(ErrorMsg + ":" + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        return result;
    }
}
