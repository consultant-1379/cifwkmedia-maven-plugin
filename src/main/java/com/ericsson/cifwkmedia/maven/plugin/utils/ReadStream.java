package com.ericsson.cifwkmedia.maven.plugin.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.maven.plugin.logging.Log;

public class ReadStream implements Runnable {
    String name;
    InputStream inputStream;
    Thread thread;
    Log log;
    String installData = "";

    public void setInstallData(String installData) {
        this.installData = installData;
    }

    public String getInstallData() {
        return installData;
    }

    public ReadStream(String name, InputStream inputStream, Log log) {
        this.name = name;
        this.inputStream = inputStream;
        this.log = log;
    }
    public void start () {
        thread = new Thread (this);
        thread.start ();
    }
    public void run () {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader (inputStream);
            BufferedReader reader = new BufferedReader (inputStreamReader);
            while (true) {
                String line = reader.readLine ();
                if (line == null) break;
                log.info(line);
                if(line.contains("Installing : ")){
                    installData += line + "\n";
                    setInstallData(installData);
                }
            }
            inputStream.close ();
            reader.close();
        } catch (Exception ex) {
            log.debug("Problem reading stream " + name + "... :" + ex);
        }
    }
}
