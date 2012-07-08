package com.betable;

import android.location.Location;
import android.os.Handler;
import com.betable.http.BetableUrl;
import com.betable.http.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods for interacting with the Betable API.
 *
 * @author Casey Crites
 */
@SuppressWarnings("unchecked")
public class Betable {

    private static final HttpClient CLIENT = new HttpClient();

    private Economy economy = Economy.SANDBOX;
    private String accessToken;
    private String gameId;

    /**
     * Query string key for the Betable access token.
     */
    public static final String ACCESS_TOKEN_KEY = "access_token";

    /**
     * int indicating a request is a user account request.
     *
     * This will be passed back to your handler so you can easily differentiate between requests.
     */
    public static final int USER_REQUEST = 0x1;

    /**
     * int indicating a request is a user wallet request.
     *
     * This will be passed back to your handler so you can easily differentiate between requests.
     */
    public static final int WALLET_REQUEST = 0x2;

    /**
     * int indicating a request is a bet request.
     *
     * This will be passed back to your handler so you can easily differentiate between requests.
     */
    public static final int BET_REQUEST = 0x3;

    /**
     * int indicating a request is a can-i-gamble request.
     *
     * This will be passed back to your handler so you can easily differentiate between requests.
     */
    public static final int GAMBLE_REQUEST = 0x4;

    /**
     * int indicating a request is an authorization request.
     *
     * This will be passed back to your handler so you can easily differentiate between requests.
     */
    public static final int AUTH_REQUEST = 0x5;

    /**
     * The available Betable economies.
     */
    public enum Economy {
        REAL, SANDBOX
    }

    /**
     * Creates a new instance of {@link Betable} with the access token set.
     *
     * A default instance of {@link Betable} is created with it's {@link Economy} set to SANDBOX.
     *
     * {@link Betable} can also be created without an access token and built up using the supplied setters, like so:
     *
     * {@code new Betable().setAccessToken("accesstoken").setEconomy(Economy.SANDBOX).setGameId("gameid");}
     *
     * @param accessToken A Betable supplied access token.
     */
    public Betable(String accessToken) {
        this.accessToken = accessToken;
    }

    // getters/setters

    /**
     * Set the access token.
     *
     * @param accessToken A Betable supplied access token.
     * @return {@link Betable}
     */
    public Betable setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    /**
     * Get the access token.
     *
     * @return The access token.
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    /**
     * Set the {@link Economy}.
     *
     * @param economy The desired {@link Economy}.
     * @return {@link Betable}
     */
    public Betable setEconomy(Economy economy) {
        this.economy = economy;
        return this;
    }

    /**
     * Get the {@link Economy}.
     *
     * @return The {@link Economy}.
     */
    public Economy getEconomy() {
        return this.economy;
    }

    /**
     * Set the game id.
     *
     * @param gameId Your game id.
     * @return {@link Betable}
     */
    public Betable setGameId(String gameId) {
        this.gameId = gameId;
        return this;
    }

    /**
     * Get the game id.
     *
     * @return The game id.
     */
    public String getGameId() {
        return this.gameId;
    }

    // requests

    /**
     * Get information about the authenticated user.
     *
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     */
    public void getUser(Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.USER_URL.get("", this.getDefaultParams()));
        CLIENT.execute(get, handler, USER_REQUEST);
    }

    /**
     * Get information about the authenticated user's wallet.
     *
     * This will contain information about the user's wallet in each Economy.
     *
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     */
    public void getUserWallet(Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.WALLET_URL.get("", this.getDefaultParams()));
        CLIENT.execute(get, handler, WALLET_REQUEST);
    }

    /**
     * Place a bet.
     *
     * This method will automatically insert the currently selected Economy into the JSON payload and
     * the current game id into the url.
     *
     * @param body A {@link org.json.JSONObject} with the bet information.
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     * @throws JSONException If the {@link Economy} is not able to be added to the body.
     */
    public void bet(JSONObject body, Handler handler) throws JSONException {
        this.bet(this.gameId, body, handler);
    }

    /**
     * Place a bet.
     *
     * This method will automatically insert the currently selected Economy into the JSON payload.
     *
     * @param gameId The game id of the game the bet is targetting.
     * @param body A {@link org.json.JSONObject} with the bet information.
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     * @throws JSONException If the {@link Economy} is not able to be added to the body.
     */
    public void bet(String gameId, JSONObject body, Handler handler) throws JSONException {
        this.checkGameId(gameId);
        HttpPost post = new HttpPost(BetableUrl.BET_URL.get(gameId, this.getDefaultParams()));
        this.addEconomyToBody(body);
        post.setEntity(new ByteArrayEntity(body.toString().getBytes()));
        post.addHeader(HttpClient.JSON_CONTENT_TYPE_HEADER);
        CLIENT.execute(post, handler, BET_REQUEST);
    }

    /**
     * Determine if the user can legally gamble based on the supplied location.
     *
     * @param location The user's current {@link android.location.Location}.
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     */
    public void canIGamble(Location location, Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.CAN_I_GAMBLE_URL.get(
            new String[] { String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()) },
            this.getDefaultParams()));
        CLIENT.execute(get, handler, GAMBLE_REQUEST);
    }

    // static requests

    /**
     * Acquire a Betable access token.
     *
     * This method is used as part of the OAuth login flow and should not be called directly.
     *
     * @param clientId A Betable client id.
     * @param clientSecret A Betable client secret.
     * @param code A Betable supplied code to exchange for an access token.
     * @param redirectUri The uri which the OAuth flow will redirect to.
     * @param handler A {@link android.os.Handler} to be called back to with the response from the server.
     * @throws AuthenticationException If authorization string cannot be generated.
     */
    public static void acquireAccessToken(String clientId, String clientSecret, String code,
                                          String redirectUri, Handler handler) throws AuthenticationException {
        HttpPost post = new HttpPost(BetableUrl.TOKEN_URL.get("", null));
        post.setEntity(createAccessTokenAcquisitionEntity(code, redirectUri));
        post.addHeader(HttpClient.FORM_CONTENT_TYPE_HEADER);
        post.addHeader(new BasicScheme().authenticate(new UsernamePasswordCredentials(clientId, clientSecret), post));
        CLIENT.execute(post, handler, AUTH_REQUEST);
    }

    // helpers

    private void addEconomyToBody(JSONObject body) throws JSONException {
        body.put("economy", this.getEconomy().toString().toLowerCase());
    }

    private static HttpEntity createAccessTokenAcquisitionEntity(String code, String redirectUri) {
        String params = URLEncodedUtils.format(createAccessTokenAcquisitionParams(code, redirectUri),
                BetableUrl.ENCODING);
        return new ByteArrayEntity(params.getBytes());
    }

    private static List<BasicNameValuePair> createAccessTokenAcquisitionParams(String code, String redirectUri) {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri", redirectUri));
        return params;
    }

    private List<BasicNameValuePair> getDefaultParams() {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(ACCESS_TOKEN_KEY, this.accessToken));
        return params;
    }

    private void checkGameId(String passedInGameId) {
        if (passedInGameId == null && this.gameId == null) {
            throw new IllegalStateException("You must either supply a game id in your call or"
                + " directly on the Betable class.");
        }
    }

}
