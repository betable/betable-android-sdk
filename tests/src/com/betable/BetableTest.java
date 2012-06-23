package com.betable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.http.HttpResponse;

import android.os.Handler;
import android.os.Message;
import android.test.AndroidTestCase;

import com.betable.Betable.Economy;
import com.betable.http.OAuth2HttpClient;

public class BetableTest extends AndroidTestCase {

    Betable betable;
    CountDownLatch latch;
    HttpResponse response;
    int responseType;
    String accessToken = "dummyaccesstoken";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        betable = new Betable(this.accessToken);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNewBetable() {
        Assert.assertTrue(this.accessToken.equals(betable.getAccessToken()));
        Assert.assertNull(this.betable.getGameId());
        Assert.assertEquals(Economy.ALL, this.betable.getEconomy());
    }

    public void testGetUser() throws InterruptedException {
        this.latch = new CountDownLatch(1);

        this.betable.getUser(new Handler() {
            @Override
            public void handleMessage(Message message) {
                BetableTest.this.responseType = message.what;
                BetableTest.this.response = (HttpResponse) message.obj;
                BetableTest.this.latch.countDown();
            }
        });

        this.latch.await(1, TimeUnit.SECONDS);

        Assert.assertEquals(0, this.latch.getCount());
        Assert.assertEquals(OAuth2HttpClient.REQUEST_RESULT, this.responseType);
        Assert.assertNotNull(this.response);
    }

    public void testGetUserWallet() throws InterruptedException {
        this.latch = new CountDownLatch(1);

        this.betable.getUserWallet(new Handler() {
            @Override
            public void handleMessage(Message message) {
                BetableTest.this.responseType = message.what;
                BetableTest.this.response = (HttpResponse) message.obj;
                BetableTest.this.latch.countDown();
            }
        });

        this.latch.await(1, TimeUnit.SECONDS);

        Assert.assertEquals(0, this.latch.getCount());
        Assert.assertEquals(OAuth2HttpClient.REQUEST_RESULT, this.responseType);
        Assert.assertNotNull(this.response);
    }

    public void testBet() throws InterruptedException {
        this.latch = new CountDownLatch(1);

        this.betable.bet(null, new Handler() {
            @Override
            public void handleMessage(Message message) {
                BetableTest.this.responseType = message.what;
                BetableTest.this.response = (HttpResponse) message.obj;
                BetableTest.this.latch.countDown();
            }
        });

        this.latch.await(1, TimeUnit.SECONDS);

        Assert.assertEquals(0, this.latch.getCount());
        Assert.assertEquals(OAuth2HttpClient.REQUEST_RESULT, this.responseType);
        Assert.assertNotNull(this.response);
    }

}
