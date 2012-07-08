package com.betable.http;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
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

import java.util.concurrent.*;

/**
 * Asynchronously execute HTTP requests.
 *
 * @author Casey Crites
 */
public class HttpClient {
    protected static final String TAG = HttpClient.class.getName();

    private static final int CORE_POOL_SIZE = 1,
            MAXIMUM_POOL_SIZE = 10,
            KEEP_ALIVE = 10;
    private static final String USER_AGENT = "Betable/0.1", CHARSET = "UTF-8";
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(MAXIMUM_POOL_SIZE),
            new BetableThreadFactory());

    private final DefaultHttpClient client;

    /**
     * A Content Type header for JSON requests.
     */
    public static final Header JSON_CONTENT_TYPE_HEADER = new BasicHeader("content-type", "application/json");

    /**
     * A Content Type header for form requests.
     */
    public static final Header FORM_CONTENT_TYPE_HEADER = new BasicHeader("content-type",
            "application/x-www-form-urlencoded");

    public static int CONNECTION_TIMEOUT = 20 * 1000,
            SOCKET_TIMEOUT = 20 * 1000,
            SOCKET_BUFFER_SIZE = 8192,
            HTTPS_PORT = 443;

    /**
     * Creates a new instance of {@link HttpClient}.
     */
    public HttpClient() {
        final HttpParams params = new BasicHttpParams();
        this.setProtocolParams(params);
        this.setConnectionParams(params);
        this.setClientParams(params);
        this.setProtocolParams(params);

        SchemeRegistry schemeRegistry = this.setUpSchemeRegistry();

        ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);
        this.client = new DefaultHttpClient(manager, params);
    }

    // actions

    /**
     * Execute an http request asynchronously.
     *
     * @param request The {@link org.apache.http.client.methods.HttpUriRequest} to be executed.
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     * @param requestType An int defined in {@link com.betable.Betable} describing what kind of request this is.
     */
    public void execute(HttpUriRequest request, Handler handler, int requestType) {
        BetableFutureTask task = new BetableFutureTask(new BetableCallable(request), handler, requestType);
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
            Log.d(TAG, this.request.getMethod() + " " + this.request.getURI().toString());
            return HttpClient.this.client.execute(this.request);
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
        final int requestType;

        public BetableFutureTask(Callable<HttpResponse> callable, Handler handler, int requestType) {
            super(callable);
            this.handler = handler;
            this.requestType = requestType;
        }

        @Override
        protected void done() {
            HttpResponse response = null;
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
                }
            }

            Message message = this.handler.obtainMessage(this.requestType, response);
            message.sendToTarget();
        }

    }

}
