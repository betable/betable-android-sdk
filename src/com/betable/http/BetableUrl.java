package com.betable.http;

import java.net.URI;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public enum BetableUrl {

    USER_URL, WALLET_URL, BET_URL, GAMBLE_URL;

    private static final String BASE_URL = "https://api.betable.com/",
            VERSION = "1.0", ENCODING = "UTF-8";
    private static final Map<BetableUrl, String> urlMap = new EnumMap<BetableUrl, String>(
            BetableUrl.class);

    static {
        urlMap.put(USER_URL, BASE_URL + VERSION + "/account");
        urlMap.put(WALLET_URL, BASE_URL + VERSION + "/account/wallet");
        urlMap.put(BET_URL, BASE_URL + VERSION + "/games/%s/bet");
        urlMap.put(GAMBLE_URL, BASE_URL + VERSION + "/can-i-gamble");
    }

    public URI get(String urlParam, List<BasicNameValuePair> params) {
        return URI.create(String.format(Locale.getDefault(), urlMap.get(this),
                urlParam) + URLEncodedUtils.format(params, ENCODING));
    }

    public URI get(Object[] urlParams, List<BasicNameValuePair> params) {
        return URI.create(String.format(Locale.getDefault(), urlMap.get(this),
                urlParams) + URLEncodedUtils.format(params, ENCODING));
    }

}
