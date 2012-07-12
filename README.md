# betable-android

Betable Android SDK

## Adding to your project

The easiest and best way is to clone this repo and add it to your project as a library. That way,
if things don't work quite the way you expect, you can modify them.

### Jar

Head to the downloads section to grab the jar.

If you want to build the jar yourself, make sure your local.properties file has the property classes.dir set to
the directory where your IDE (or whatever you use) compiles the betable code, then from the betable-android
directory, type:

    ant jar

and you'll find the jar in the bin/ directory.

### Dependencies

* android-support-v4.jar

### Permissions

* android.permission.INTERNET
* android.permission.ACCESS_COARSE_LOCATION

**NOTE** The location permission isn't technically required for the library, but the can-i-gamble
and bet endpoints require a current location for compliance reasons.

## BetableLogin

The BetableLogin class is a Fragment that will take the user through the Betable OAuth2 login flow.

### BetableLogin.BetableLoginListener

Before you start adding BetableLogin to your Activity, you need to make sure your Activity implements this interface.
This interface declares two methods:

    public void onSuccessfulLogin(String accessToken);
    public void onFailedLogin(String reason);

that BetableLogin will use to communicate with your Activity when the login succeeds or fails.

### Adding BetableLogin to your Activity

BetableLogin can be added to your Activity in two ways:

1. Embedded directly in your layout xml

        <fragment class="com.betable.fragment.BetableLogin"
            android:id="@+id/betable_login_fragment"
            android:layout_width="fill_parent"
            android:layout_height="fill_parent"/>

1. Created dynamically at runtime

        this.betableLogin = BetableLogin.newInstance(clientId, clientSecret, redirectUri);
        this.betableLogin.show(this.getSupportFragmentManager(), containerLayoutId, betableLoginTag);

   and dismissed

        this.betableLogin.dismiss();

All configuration changes are handled by BetableLogin, so you don't need to worry about doing anything special for
screen rotations or keyboard slides.

## Betable

The Betable class is where we interact with the Betable API.

It can be instantiated one of two ways:

1. With an access token

        Betable betable = new Betable(accessToken);

1. Or empty

        Betable betable = new Betable();

    and then build it up

        betable.setAccessToken(accessToken).setGameId(gameId).setEconomy(Economy.SANDBOX);

Once you have your instance of Betable, you can start making API calls

    betable.getUserWallet(this.someHandler);

Betable will handle all the threading for you and return a message to your handler when it's done. A possible
(contrived) implementation of your Handler may look like this

    this.someHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int requestType = message.what;
            HttpResponse response = (HttpResponse) message.response;

            switch(requestType) {
                case Betable.USER_REQUEST:
                    handleUserResponse(response);
                    break;
                case Betable.WALLET_REQUEST:
                    handleWalletResponse(response);
                    break;
            }

        }
    }

**NOTE** Remember that Handler's won't survive screen rotations, so make sure you keep your Handler safe by passing it to
back to your Activity by returning it in activity.onRetainCustomNonConfigurationInstance() and retrieving it using
activity.getLastCustomNonConfigurationInstance(). If that seems like a lot of work, you can create your handler on
some abstract class and access it there.
