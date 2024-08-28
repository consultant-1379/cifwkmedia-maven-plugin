package com.ericsson.cifwkmedia.maven.plugin;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

class WebClientWrapper {
    private static final String protocol = "https";
    private static final int port = 443;
    private static Log log;

    public static HttpClient wrapClient(HttpClient base) {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            X509TrustManager trustManager = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xc, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslcontext.init(null, new TrustManager[] { trustManager }, null);
            SSLSocketFactory factory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager clientConnectionManager = base.getConnectionManager();
            SchemeRegistry schemeRegister = clientConnectionManager.getSchemeRegistry();
            schemeRegister.register(new Scheme(protocol, port, factory));
            return new DefaultHttpClient(clientConnectionManager, base.getParams());
        } catch (Exception error) {
            log.error("Error details: ", error);
            try {
                throw new MojoFailureException("Error writing contents to local media file :" + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
                return null;
            }

        }
    }
}
