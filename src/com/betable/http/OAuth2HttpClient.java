package com.betable.http;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

public class OAuth2HttpClient {
    protected static final String TAG = OAuth2HttpClient.class.getName();

    public static final Header JSON_CONTENT_TYPE_HEADER = new BasicHeader(
            "content-type", "application/json");
    public static final Header FORM_CONTENT_TYPE_HEADER = new BasicHeader(
            "content-type", "application/x-www-form-urlencoded");
    public static final int REQUEST_RESULT = 0x1, REQUEST_ERRED = 0x2;

    public static int CONNECTION_TIMEOUT = 20 * 1000,
            SOCKET_TIMEOUT = 20 * 1000, SOCKET_BUFFER_SIZE = 8192,
            HTTPS_PORT = 443;

    private static final int CORE_POOL_SIZE = 1, MAXIMUM_POOL_SIZE = 10,
            KEEP_ALIVE = 10;
    private static final String USER_AGENT = "Betable/0.1", CHARSET = "UTF-8";
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(MAXIMUM_POOL_SIZE),
            new BetableThreadFactory());

    private final DefaultHttpClient client;

    public OAuth2HttpClient() {
        final HttpParams params = new BasicHttpParams();
        this.setProtocolParams(params);
        this.setConnectionParams(params);
        this.setClientParams(params);
        this.setProtocolParams(params);

        SchemeRegistry schemeRegistry = this.setUpSchemeRegistry();

        ClientConnectionManager manager = new ThreadSafeClientConnManager(
                params, schemeRegistry);
        this.client = new DefaultHttpClient(manager, params);
    }

    // actions

    public void execute(HttpUriRequest request, Handler handler) {
        BetableFutureTask task = new BetableFutureTask(new BetableCallable(
                request), handler);
        EXECUTOR.execute(task);
    }

    // helpers

    private void setProtocolParams(HttpParams params) {
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, CHARSET);
        HttpProtocolParams.setUserAgent(params, USER_AGENT);
    }

    private void setConnectionParams(HttpParams params) {
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, SOCKET_BUFFER_SIZE);
    }

    private void setClientParams(HttpParams params) {
        HttpClientParams.setRedirecting(params, false);
    }

    private SchemeRegistry setUpSchemeRegistry() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", SSLSocketFactory
                .getSocketFactory(), HTTPS_PORT));
        return schemeRegistry;
    }

    // inner-classes

    class BetableCallable implements Callable<HttpResponse> {

        HttpUriRequest request;

        public BetableCallable(HttpUriRequest request) {
            this.request = request;
        }

        @Override
        public HttpResponse call() throws Exception {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Log.d(TAG, this.request.getMethod() + " "
                    + this.request.getURI().toString());
            return OAuth2HttpClient.this.client.execute(this.request);
        }
    }

    static class BetableThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable);
        }
    }

    static class BetableFutureTask extends FutureTask<HttpResponse> {

        final Handler handler;

        public BetableFutureTask(Callable<HttpResponse> callable,
                Handler handler) {
            super(callable);
            this.handler = handler;
        }

        @Override
        protected void done() {
            HttpResponse response = null;
            int responseType = REQUEST_RESULT;
            String errorString = null;
            try {
                response = this.get();
            } catch (InterruptedException e) {
                errorString = e.getMessage();
            } catch (ExecutionException e) {
                errorString = e.getMessage();
            } finally {
                if (errorString != null) {
                    Log.e(TAG, errorString);
                    responseType = REQUEST_ERRED;
                }
            }
            Log.d(TAG, "status code: "
                    + response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() >= 400) {
                responseType = REQUEST_ERRED;
            }
            Message message = this.handler
                    .obtainMessage(responseType, response);
            message.sendToTarget();
        }

    }

}
