package com.betable.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.betable.Betable;
import com.betable.R;
import com.betable.http.BetableUrl;
import com.betable.http.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Betable OAuth login flow.
 *
 * @author Casey Crites
 */
public class BetableLogin extends Fragment {
    private static final String TAG = "BetableLogin";

    private static final String CODE_KEY = "code",
            ERROR_KEY = "error",
            ACCESS_TOKEN_KEY = "access_token",
            CLIENT_ID_KEY = "client_id",
            CLIENT_SECRET_KEY = "client_secret",
            REDIRECT_URI_KEY = "redirect_uri",
            STATE_KEY = "state",
            RESPONSE_KEY = "response",
            RESPONSE_VALUE = "code";

    boolean pageLoaded = false;
    boolean showProgressDialog = false;
    BetableLoginListener listener;
    ProgressDialog progressDialog;
    WebView browser;

    /**
     * Create a new instance of {@link BetableLogin}.
     *
     * This method should be used instead of the constructor when dynamically adding {@link BetableLogin} to a layout.
     *
     * @param clientId A Betable client id.
     * @param clientSecret A Betable client secret.
     * @param redirectUri The uri which the OAuth flow will redirect to.
     * @return An instance of {@link BetableLogin}.
     */
    public static BetableLogin newInstance(String clientId, String clientSecret, String redirectUri) {
        BetableLogin login = new BetableLogin();
        login.setArguments(createInitialArguments(clientId, clientSecret, redirectUri));
        return login;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.setActivityAsListener(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (this.browser == null) {
            this.browser = (WebView) inflater.inflate(R.layout.betable_login, container, false);
        } else {
            ((ViewGroup)this.browser.getParent()).removeView(this.browser);
            this.browser.setLayoutParams(container.getLayoutParams());
        }
        return this.browser;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!this.pageLoaded) {
            this.initializeBrowser();
            this.browser.loadUrl(BetableUrl.AUTHORIZATION_URL.getAsString("", this.createAuthQueryParams()));
            this.pageLoaded = true;
        } else {
            this.browser.restoreState(savedInstanceState);
        }
        this.showProgressDialog();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.dismissProgressDialog(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (this.browser != null) {
            this.browser.saveState(savedInstanceState);
        }
    }

    // actions

    /**
     * Display an instance of {@link BetableLogin}.
     *
     * Use this method when dynamically adding {@link BetableLogin} to a layout.
     *
     * @param manager The support library {@link android.support.v4.app.FragmentManager}.
     * @param layoutId The id of the {@link BetableLogin} layout.
     * @param tag A String that can be used to find {@link BetableLogin} using the {@link android.support.v4.app.FragmentManager}.
     */
    public void show(FragmentManager manager, int layoutId, String tag) {
        manager.beginTransaction().add(layoutId, this, tag).commit();
    }

    /**
     * Dismiss the {@link BetableLogin} fragment.
     *
     * Use this method when you want to dismiss a dynamically added instance of {@link BetableLogin}.
     */
    public void dismiss() {
        this.getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    // helpers

    private void cleanUp() {
        this.pageLoaded = false;
        this.browser = null;
    }

    private void showProgressDialog() {
        if (this.showProgressDialog && (this.progressDialog == null || !this.progressDialog.isShowing())) {
            this.progressDialog = ProgressDialog.show(this.getActivity(), "Betable", "Please wait...");
        }
    }

    private void dismissProgressDialog(boolean restartDialogOnRecreate) {
        this.showProgressDialog = restartDialogOnRecreate;
        if (this.progressDialog != null && this.progressDialog.isShowing()) {
            this.progressDialog.dismiss();
        }
    }

    private String getArgument(String key) {
        return this.getArguments().getString(key);
    }

    private static Bundle createInitialArguments(String clientId, String clientSecret, String redirectUri) {
        Bundle arguments = new Bundle();
        arguments.putString(CLIENT_ID_KEY, clientId);
        arguments.putString(CLIENT_SECRET_KEY, clientSecret);
        arguments.putString(REDIRECT_URI_KEY, redirectUri);
        arguments.putString(STATE_KEY, UUID.randomUUID().toString());
        return arguments;
    }

    private List<BasicNameValuePair> createAuthQueryParams() {
        List<BasicNameValuePair> queryParams = new ArrayList<BasicNameValuePair>();
        queryParams.add(new BasicNameValuePair(STATE_KEY, this.getArgument(STATE_KEY)));
        queryParams.add(new BasicNameValuePair(RESPONSE_KEY, RESPONSE_VALUE));
        queryParams.add(new BasicNameValuePair(CLIENT_ID_KEY, this.getArgument(CLIENT_ID_KEY)));
        queryParams.add(new BasicNameValuePair(REDIRECT_URI_KEY, this.getArgument(REDIRECT_URI_KEY)));
        return queryParams;
    }

    private void initializeBrowser() {
        this.browser.getSettings().setJavaScriptEnabled(true);

        this.browser.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "Started loading " + url);
                if (url.startsWith(BetableLogin.this.getArgument(REDIRECT_URI_KEY))) {
                    view.stopLoading();
                }
                BetableLogin.this.showProgressDialog = true;
                BetableLogin.this.showProgressDialog();
                Uri uri = Uri.parse(url);
                if (uri.getQueryParameter(ERROR_KEY) != null) {
                    this.handleAccessDenied(uri);
                } else if (uri.getQueryParameter(CODE_KEY) != null) {
                    this.handleAccessGranted(uri);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Finished loading " + url);
                if (!url.startsWith(BetableLogin.this.getArgument(REDIRECT_URI_KEY))) {
                    BetableLogin.this.dismissProgressDialog(false);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(TAG, "Received error code " + String.valueOf(errorCode) + " when trying to load "
                    + failingUrl);
                BetableLogin.this.dismissProgressDialog(false);
                BetableLogin.this.listener.onFailedLogin(description);
        }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            // helpers

            private void handleAccessDenied(Uri uri) {
                BetableLogin.this.cleanUp();
                BetableLogin.this.dismissProgressDialog(false);
                BetableLogin.this.listener.onFailedLogin(uri.getQueryParameter(ERROR_KEY));
            }

            private void handleAccessGranted(Uri uri) {
                if (!doesStateMatch(uri)) {
                    Log.e(TAG, "State did not match, possible CSRF attack. Returning.");
                    BetableLogin.this.listener.onFailedLogin("State did not match, possible CSRF attack.");
                    return;
                }
                String code = uri.getQueryParameter(CODE_KEY);
                try {
                    Betable.acquireAccessToken(BetableLogin.this.getArgument(CLIENT_ID_KEY),
                        BetableLogin.this.getArgument(CLIENT_SECRET_KEY), code,
                        BetableLogin.this.getArgument(REDIRECT_URI_KEY),
                        BetableLogin.this.accessTokenHandler);
                } catch (AuthenticationException e) {
                    Log.e(TAG, e.getMessage());
                    throw new IllegalStateException("Credentials are invalid.", e);
                }
            }

            private boolean doesStateMatch(Uri uri) {
                return BetableLogin.this.getArgument(STATE_KEY).equals(uri.getQueryParameter(STATE_KEY));
            }

        });
   }

    private void setActivityAsListener(Activity activity) {
        try {
            this.listener = (BetableLoginListener) activity;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Activities implementing " + TAG
                + " must implement the " + TAG + ".BetableLoginListener interface.");
        }
    }

    private String parseAccessToken(HttpEntity entity) throws IOException, ParseException, JSONException {
        JSONObject responseBody = new JSONObject(EntityUtils.toString(entity, BetableUrl.ENCODING));
        return responseBody.getString(ACCESS_TOKEN_KEY);
    }

    private void handleSuccessfulRequest(HttpResponse response) {
        String errorString = null;
        BetableLogin.this.dismissProgressDialog(false);
        try {
            BetableLogin.this.listener.onSuccessfulLogin(BetableLogin.this.parseAccessToken(response.getEntity()));
        } catch (IOException e) {
            errorString = e.getMessage();
        } catch (JSONException e) {
            errorString = e.getMessage();
        } catch (ParseException e) {
            errorString = e.getMessage();
        } finally {
            if (errorString != null) {
                Log.e(TAG, errorString);
                BetableLogin.this.listener.onFailedLogin(errorString);
            }
        }
    }

    private void handleFailedRequest(HttpResponse response) {
        String responseBody = null;
        String errorString = null;
        try {
            responseBody = EntityUtils.toString(response.getEntity());
        } catch (ParseException e) {
            errorString = e.getMessage();
        } catch (IOException e) {
            errorString = e.getMessage();
        } finally {
            if (errorString != null) {
                Log.e(TAG, errorString);
                responseBody = errorString;
            }
        }
        BetableLogin.this.dismissProgressDialog(false);
        BetableLogin.this.listener.onFailedLogin(responseBody);
    }

    // inner-classes

    Handler accessTokenHandler = new Handler() {

        @Override
        public void handleMessage(Message message) {
            int resultType = message.what;
            HttpResponse response = (HttpResponse) message.obj;

            BetableLogin.this.cleanUp();
            switch (resultType) {
                case HttpClient.REQUEST_RESULT:
                    BetableLogin.this.handleSuccessfulRequest(response);
                    break;
                case HttpClient.REQUEST_ERRED:
                    BetableLogin.this.handleFailedRequest(response);
                    break;
            }
        }
    };

    // interfaces

    /**
     * Listen for {@link BetableLogin} results.
     *
     * Every activity that displays BetableLogin must implement this interface.
     */
    public interface BetableLoginListener {
        /**
         * Called when {@link BetableLogin} finishes successfully.
         *
         * @param accessToken A Betable supplied access token.
         */
        public void onSuccessfulLogin(String accessToken);

        /**
         * Called when {@link BetableLogin} fails.
         *
         * @param reason A String attempting to explain why {@link BetableLogin} failed.
         */
        public void onFailedLogin(String reason);
    }

}
