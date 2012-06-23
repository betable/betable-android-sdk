package com.betable;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
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

    private final OAuth2HttpClient client;

    private Economy economy = Economy.ALL;
    private ArrayList<BasicNameValuePair> params;
    private String accessToken;
    private String gameId;

    public static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN",
            GAME_ID_KEY = "GAME_ID";

    public Betable(String accessToken) {
        this.accessToken = accessToken;
        this.initializeParams();
        this.client = new OAuth2HttpClient();
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
        HttpGet get = new HttpGet(BetableUrl.USER_URL.get("",
                (List<BasicNameValuePair>) this.params.clone()));
        this.client.execute(get, handler);
    }

    public void getUserWallet(Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.WALLET_URL.get("",
                (List<BasicNameValuePair>) this.params.clone()));
        this.client.execute(get, handler);
    }

    public void bet(JSONObject body, Handler handler) {
        this.bet(this.gameId, body, handler);
    }

    public void bet(String gameId, JSONObject body, Handler handler) {
        this.checkGameId(gameId);
        HttpPost post = new HttpPost(BetableUrl.BET_URL.get(gameId,
                (List<BasicNameValuePair>) this.params.clone()));
        post.setEntity(new ByteArrayEntity(body.toString().getBytes()));
        post.addHeader(OAuth2HttpClient.CONTENT_TYPE_HEADER);
        this.client.execute(post, handler);
    }

    public void canIGamble(Location location, Handler handler) {
        HttpGet get = new HttpGet(BetableUrl.GAMBLE_URL.get("",
                (List<BasicNameValuePair>) this.params.clone()));
        this.client.execute(get, handler);
    }

    // helpers

    private void initializeParams() {
        this.params = new ArrayList<BasicNameValuePair>();
        this.params.add(new BasicNameValuePair(ACCESS_TOKEN_KEY,
                this.accessToken));
    }

    private void checkGameId(String passedInGameId) {
        if (passedInGameId == null && this.gameId == null) {
            throw new IllegalStateException(
                    "You must either supply a game id in your call or"
                            + " directly on the Betable class.");
        }
    }

}
