package com.ericsson.cifwkmedia.maven.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public class GenericRestCalls {

    private HttpResponse response = null;
    public String paramsString;
    public String result = "";

    public String setUpGETRestCall(List<NameValuePair> params, String restUrl, Log log) throws MojoExecutionException, MojoFailureException {
        try {
            HttpClient client = new DefaultHttpClient();
            client = new DefaultHttpClient();
            client = WebClientWrapper.wrapClient(client);
            paramsString = URLEncodedUtils.format(params, "UTF-8");
            HttpGet request = new HttpGet(restUrl + "?" + paramsString + "&pretty=true");
            response = client.execute(request);
        } catch (IOException IOerror) {
            log.error("Error with setUpGETRestCall: " + IOerror);
            throw new MojoFailureException("Error trying to create a UrlEncodedFormEntity with db information " + IOerror);
        }
        try {
            log.info("*** Executing Rest GET to CIFWK DB ***");
            log.info("Rest Call GET: " + restUrl + "?" + paramsString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                result += line + "\r\n";
                if (line.toLowerCase().contains("error")) {
                    throw new MojoFailureException("Error getting information from cifwk DB :" + line);
                }
                log.info(line);
            }
        } catch (IOException IOerror) {
            log.error("Error with setUpGETRestCall execute on the client: ", IOerror);
            throw new MojoFailureException("Error posting information to the cifwk DB :" + IOerror);
        }
        return result;
    }

    public void setUpPOSTRestCall(List<BasicNameValuePair> params, String restUrl, Log log) throws MojoExecutionException, MojoFailureException {
        HttpPost post = null;
        try {
            HttpClient client = new DefaultHttpClient();
            client = WebClientWrapper.wrapClient(client);
            post = new HttpPost(restUrl);
            post.setEntity(new UrlEncodedFormEntity(params));
            log.info("*** Executing Rest POST to CIFWK DB ***");
            log.info("Rest Call POST: " + post.toString() + " with parameters: " + params);
            response = client.execute(post);
        } catch (IOException IOerror) {
            log.error("Error with setUpPOSTRestCall: " + IOerror);
            throw new MojoFailureException("Error with setUpPOSTRestCall: " + IOerror);
        }

        try {
            log.info("*** Get Content of Post Response ***");
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                if (line.toLowerCase().contains("error")) {
                    throw new MojoFailureException("Error posting information to the cifwk DB :" + line);
                }
                log.info(line);
            }
        } catch (IOException IOerror) {
            log.error("Error with setUpPOSTRestCall execute on the client: ", IOerror);
            throw new MojoFailureException("Error posting information to the cifwk DB :" + IOerror);
        }
    }
}
