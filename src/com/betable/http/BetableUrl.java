package com.betable.http;

import java.net.URI;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public enum BetableUrl {

    USER_URL, WALLET_URL, BET_URL, GAMBLE_URL, AUTHORIZATION_URL;

    private static final String BASE_API_URL = "https://api.betable.com/",
            BASE_AUTH_URL = "https://betable.com/authorize", VERSION = "1.0",
            ENCODING = "UTF-8";
    private static final Map<BetableUrl, String> urlMap = new EnumMap<BetableUrl, String>(
            BetableUrl.class);

    static {
        urlMap.put(USER_URL, BASE_API_URL + VERSION + "/account");
        urlMap.put(WALLET_URL, BASE_API_URL + VERSION + "/account/wallet");
        urlMap.put(BET_URL, BASE_API_URL + VERSION + "/games/%s/bet");
        urlMap.put(GAMBLE_URL, BASE_API_URL + VERSION + "/can-i-gamble");
        urlMap.put(AUTHORIZATION_URL, BASE_AUTH_URL);
    }

    public URI get(String urlParam, List<BasicNameValuePair> params) {
        return URI.create(this.getAsString(urlParam, params));
    }

    public String getAsString(String urlParam, List<BasicNameValuePair> params) {
        return String.format(Locale.getDefault(), urlMap.get(this), urlParam)
                + URLEncodedUtils.format(params, ENCODING);
    }

    public URI get(Object[] urlParams, List<BasicNameValuePair> params) {
        return URI.create(this.getAsString(urlParams, params));
    }

    public String getAsString(Object[] urlParams,
            List<BasicNameValuePair> params) {
        return String.format(Locale.getDefault(), urlMap.get(this), urlParams)
                + URLEncodedUtils.format(params, ENCODING);
    }

}
