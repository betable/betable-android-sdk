package com.betable;

import java.util.concurrent.CountDownLatch;

import android.location.Location;
import android.os.Looper;
import android.test.InstrumentationTestCase;
import junit.framework.Assert;

import org.apache.http.HttpResponse;

import android.os.Handler;
import android.os.Message;

import com.betable.Betable.Economy;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BetableTest extends InstrumentationTestCase {

    Betable betable;
    CountDownLatch latch;
    HttpResponse response;
    int responseType;
    String accessToken = "your access token";
    String gameId = "your game id";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Note: I'm not your dad or anything, but I wouldn't change the Economy to REAL here.
        betable = new Betable(this.accessToken).setGameId(this.gameId);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        this.response = null;
        this.responseType = -1;
    }

    public void testNewBetable() {
        Assert.assertTrue(this.accessToken.equals(betable.getAccessToken()));
        Assert.assertNotNull(this.betable.getGameId());
        Assert.assertEquals(Economy.SANDBOX, this.betable.getEconomy());
    }

    public void testGetUser() throws Throwable {
        this.latch = new CountDownLatch(1);

        new LooperThread() {
            @Override
            public void run() {
                BetableTest.this.betable.getUser(new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        BetableTest.this.responseType = message.what;
                        BetableTest.this.response = (HttpResponse) message.obj;
                        BetableTest.this.latch.countDown();
                    }
                });
            }
        }.start();

        this.latch.await();

        Assert.assertEquals(0, this.latch.getCount());
        Assert.assertEquals(Betable.USER_REQUEST, this.responseType);
        Assert.assertNotNull(this.response);
    }

    public void testGetUserWallet() throws InterruptedException {
        this.latch = new CountDownLatch(1);

        new LooperThread() {
            @Override
            public void run() {
                BetableTest.this.betable.getUserWallet(new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        BetableTest.this.responseType = message.what;
                        BetableTest.this.response = (HttpResponse) message.obj;
                        BetableTest.this.latch.countDown();
                    }
                });
            }
        }.start();

        this.latch.await();

        Assert.assertEquals(0, this.latch.getCount());
        Assert.assertEquals(Betable.WALLET_REQUEST, this.responseType);
        Assert.assertNotNull(this.response);
    }

    public void testBet() throws InterruptedException {
        this.latch = new CountDownLatch(1);

        new LooperThread() {
            @Override
            public void run() {
                try {
                    BetableTest.this.betable.bet(BetableTest.this.buildBetPayload(), new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                            BetableTest.this.responseType = message.what;
                            BetableTest.this.response = (HttpResponse) message.obj;
                            BetableTest.this.latch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    Assert.fail(e.getMessage());
                }
            }
        }.start();

        this.latch.await();

        Assert.assertEquals(Betable.BET_REQUEST, this.responseType);
        Assert.assertNotNull(this.response);
    }

    public void testCanIGamble() throws InterruptedException {
        this.latch = new CountDownLatch(1);

        // San Francisco
        final Location location = new Location("fakeprovider");
        location.setLatitude(37.7750);
        location.setLongitude(122.4183);

        new LooperThread() {
            @Override
            public void run() {
                BetableTest.this.betable.canIGamble(location, new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        BetableTest.this.responseType = message.what;
                        BetableTest.this.response = (HttpResponse) message.obj;
                        BetableTest.this.latch.countDown();
                    }
                });
            }
        }.start();

        this.latch.await();
        Assert.assertEquals(Betable.GAMBLE_REQUEST, this.responseType);
        Assert.assertNotNull(this.response);
    }

    // helpers

    private JSONObject buildBetPayload() throws JSONException {
        JSONObject bet = new JSONObject();
        bet.put("wager", "1.00");
        bet.put("currency", "GBP");
        bet.put("paylines", new JSONArray("[[1,1,1],[2,2,2]]"));
        bet.put("location", String.valueOf("37.7607,-122.4842"));
        return bet;
    }

    // inner-classes

    static class LooperThread extends Thread {

        public LooperThread() {
        }

        @Override
        public void start() {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    LooperThread.this.run();
                    Looper.loop();
                }
            }.start();
        }
    }

}
