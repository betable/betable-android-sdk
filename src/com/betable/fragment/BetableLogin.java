package com.betable.fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.betable.Betable;
import com.betable.R;
import com.betable.http.BetableUrl;
import com.betable.http.OAuth2HttpClient;

public class BetableLogin extends DialogFragment {
    private static final String TAG = "BetableLogin";

    private static final String CODE_KEY = "code", ERROR_KEY = "error",
            ACCESS_TOKEN_KEY = "access_token";

    BetableLoginListener listener;
    ProgressDialog loadingDialog;
    String clientId;
    String clientSecret;
    String redirectUri;
    WebView browser;

    public BetableLogin(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.setActivityAsListener(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        this.browser = (WebView) inflater.inflate(R.layout.betable_login,
                container, false);
        this.initializeBrowser();
        this.browser.loadUrl(BetableUrl.AUTHORIZATION_URL.getAsString("",
                this.createAuthQueryParams()));
        return this.browser;
    }

    // helpers

    private List<BasicNameValuePair> createAuthQueryParams() {
        List<BasicNameValuePair> queryParams = new ArrayList<BasicNameValuePair>();
        queryParams.add(new BasicNameValuePair("state", UUID.randomUUID()
                .toString()));
        queryParams.add(new BasicNameValuePair("response", "code"));
        queryParams.add(new BasicNameValuePair("client_id", this.clientId));
        queryParams
                .add(new BasicNameValuePair("redirect_uri", this.redirectUri));
        return queryParams;
    }

    private void initializeBrowser() {
        this.browser.getSettings().setJavaScriptEnabled(true);

        this.browser.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "Started loading " + url);
                Uri uri = Uri.parse(url);

                if (uri.getQueryParameter(ERROR_KEY) != null) {
                    BetableLogin.this.listener.onFailedLogin(uri
                            .getQueryParameter(ERROR_KEY));
                    return;
                } else if (uri.getQueryParameter(CODE_KEY) != null) {
                    String code = uri.getQueryParameter(CODE_KEY);
                    view.stopLoading();
                    BetableLogin.this.loadingDialog = ProgressDialog.show(
                            BetableLogin.this.getActivity(), "Betable",
                            "Please wait...");
                    try {
                        Betable.acquireAccessToken(BetableLogin.this.clientId,
                                BetableLogin.this.clientSecret, code, url,
                                new AccessTokenHandler());
                    } catch (AuthenticationException e) {
                        Log.e(TAG, e.getMessage());
                        throw new IllegalStateException(
                                "Client key/secret are invalid.", e);
                    }
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

        });
    }

    private void setActivityAsListener(Activity activity) {
        if (!BetableLoginListener.class.isInstance(activity)) {
            throw new IllegalStateException("Activities implementing " + TAG
                    + " must implement the " + TAG
                    + ".BetableLoginListener interface.");
        }
        this.listener = (BetableLoginListener) activity;
    }

    private String parseAccessToken(HttpEntity entity) throws IOException {
        List<NameValuePair> params = URLEncodedUtils.parse(entity);
        String accessToken = null;
        for (NameValuePair param : params) {
            if (param.getName().equals(ACCESS_TOKEN_KEY)) {
                accessToken = param.getValue();
                break;
            }
        }
        return accessToken;
    }

    // inner-classes

    class AccessTokenHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            int resultType = message.what;
            HttpResponse response = (HttpResponse) message.obj;
            String errorString = null;

            switch (resultType) {
            case OAuth2HttpClient.REQUEST_RESULT:
                try {
                    BetableLogin.this.listener
                            .onSuccessfulLogin(BetableLogin.this
                                    .parseAccessToken(response.getEntity()));
                    break;
                } catch (IOException e) {
                    errorString = e.getMessage();
                    Log.e(TAG, errorString);
                }
            case OAuth2HttpClient.REQUEST_ERRED:
                if (errorString != null) {
                    BetableLogin.this.listener.onFailedLogin(errorString);
                } else {
                    String responseBody = null;
                    try {
                        responseBody = EntityUtils.toString(response
                                .getEntity());
                    } catch (ParseException e) {
                        errorString = e.getMessage();
                    } catch (IOException e) {
                        errorString = e.getMessage();
                    } finally {
                        if (errorString != null) {
                            Log.e(TAG, errorString);
                        }
                    }
                    BetableLogin.this.listener
                            .onFailedLogin(responseBody == null ? errorString
                                    : responseBody);
                }
                break;
            }
            if (BetableLogin.this.loadingDialog.isShowing())
                BetableLogin.this.loadingDialog.dismiss();
        }
    }

    // interfaces

    public interface BetableLoginListener {
        public void onSuccessfulLogin(String accessToken);

        public void onFailedLogin(String reason);
    }

}
