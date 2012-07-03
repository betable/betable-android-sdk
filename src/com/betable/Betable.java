package com.betable;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.location.Location;
import android.os.Handler;

import com.betable.http.BetableUrl;
import com.betable.http.OAuth2HttpClient;

@SuppressWarnings("unchecked")
public class Betable {

    public enum Economy {
        ALL, REAL, SANDBOX
    }

    private static final OAuth2HttpClient CLIENT = new OAuth2HttpClient();

    private Economy economy = Economy.ALL;
    private ArrayList<BasicNameValuePair> params;
    private String accessToken;
    private String gameId;

    public static final String ACCESS_TOKEN_KEY = "access_token",
            GAME_ID_KEY = "game_id";

    public Betable(String accessToken) {
        this.accessToken = accessToken;
        this.initializeParams();
    }

    // getters/setters

    public Betable setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public Betable setEconomy(Economy economy) {
        this.economy = economy;
        return this;
    }

    public Economy getEconomy() {
        return this.economy;
    }

    public Betable setGameId(String gameId) {
        this.gameId = gameId;
        return this;
    }

    public String getGameId() {
        return this.gameId;
    }

    // requests

    public void getUser(Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.USER_URL.get("", (List<BasicNameValuePair>) this.params.clone()));
        CLIENT.execute(get, handler);
    }

    public void getUserWallet(Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.WALLET_URL.get("", (List<BasicNameValuePair>) this.params.clone()));
        CLIENT.execute(get, handler);
    }

    public void bet(JSONObject body, Handler handler) {
        this.bet(this.gameId, body, handler);
    }

    public void bet(String gameId, JSONObject body, Handler handler) {
        this.checkGameId(gameId);
        HttpPost post = new HttpPost(BetableUrl.BET_URL.get(gameId, (List<BasicNameValuePair>) this.params.clone()));
        post.setEntity(new ByteArrayEntity(body.toString().getBytes()));
        post.addHeader(OAuth2HttpClient.JSON_CONTENT_TYPE_HEADER);
        CLIENT.execute(post, handler);
    }

    public void canIGamble(Location location, Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.CAN_I_GAMBLE_URL.get(
                new String[] { String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()) },
                (List<BasicNameValuePair>) this.params.clone()));
        CLIENT.execute(get, handler);
    }

    // static actions

    public static void acquireAccessToken(String clientId, String clientSecret, String code,
                                          String redirectUri, Handler handler) throws AuthenticationException {
        HttpPost post = new HttpPost(BetableUrl.TOKEN_URL.get("", null));
        post.setEntity(createAccessTokenAcquisitionEntity(code, redirectUri));
        post.addHeader(OAuth2HttpClient.FORM_CONTENT_TYPE_HEADER);
        post.addHeader(new BasicScheme().authenticate(new UsernamePasswordCredentials(clientId, clientSecret), post));
        CLIENT.execute(post, handler);
    }

    // helpers

    private static HttpEntity createAccessTokenAcquisitionEntity(String code,
            String redirectUri) {
        String params = URLEncodedUtils.format(createAccessTokenAcquisitionParams(code, redirectUri),
                BetableUrl.ENCODING);
        return new ByteArrayEntity(params.getBytes());
    }

    private static List<BasicNameValuePair> createAccessTokenAcquisitionParams(
            String code, String redirectUri) {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri", redirectUri));
        return params;
    }

    private void initializeParams() {
        this.params = new ArrayList<BasicNameValuePair>();
        this.params.add(new BasicNameValuePair(ACCESS_TOKEN_KEY, this.accessToken));
    }

    private void checkGameId(String passedInGameId) {
        if (passedInGameId == null && this.gameId == null) {
            throw new IllegalStateException("You must either supply a game id in your call or"
                            + " directly on the Betable class.");
        }
    }

}
