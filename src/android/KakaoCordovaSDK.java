package com.needer.plugin.kakao;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.kakao.auth.ApprovalType;
import com.kakao.auth.AuthType;
import com.kakao.auth.IApplicationConfig;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.ISessionConfig;
import com.kakao.auth.KakaoAdapter;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.ButtonObject;
import com.kakao.message.template.ContentObject;
import com.kakao.message.template.FeedTemplate;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.SocialObject;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class KakaoCordovaSDK extends CordovaPlugin {
  @SuppressLint("StaticFieldLeak")
  private static volatile Activity currentActivity;
  private static final String LOG_TAG = "KakaoCordovaSDK";

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    Log.v(LOG_TAG, "kakao plugin init");

    super.initialize(cordova, webView);

    currentActivity = this.cordova.getActivity();

    KakaoSDK.init(new KakaoSDKAdapter());
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    Log.v(LOG_TAG, "kakao : execute " + action);
    cordova.setActivityResultCallback(this);
    removeSessionCallback();

    switch (action) {
      case "login":
        this.login(callbackContext);
        return true;
      case "logout":
        this.logout(callbackContext);
        return true;
      case "unlinkApp":
        this.unlinkApp(callbackContext);
        return true;
      case "sendLinkFeed":
        this.sendLinkFeed(args, callbackContext);
        return true;
      case "sendLinkCustom":
        this.sendLinkCustom(args, callbackContext);
        return true;
      case "getAccessToken":
        this.getAccessToken(callbackContext);
        return true;
    }

    return false;
  }

  private void removeSessionCallback() {
    Session.getCurrentSession().clearCallbacks();
  }

  public static Activity getCurrentActivity() {
    return currentActivity;
  }

  private void login(CallbackContext callbackContext) {
    cordova.getActivity().runOnUiThread(() -> {
      try {
        Session.getCurrentSession().addCallback(new SessionCallback(callbackContext));
        Session.getCurrentSession().open(AuthType.KAKAO_TALK, cordova.getActivity());
      } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error("카카오 로그인 실패");
      }
    });
  }

  private LinkObject getLinkObject(JSONObject object) {
    if (object == null) {
      return null;
    }

    LinkObject.Builder linkObjectBuilder = new LinkObject.Builder();

    try {
      if (object.has("webURL")) {
        linkObjectBuilder.setWebUrl("webURL");
      }

      if (object.has("mobileWebURL")) {
        linkObjectBuilder.setMobileWebUrl("mobileWebURL");
      }

      if (object.has("androidExecutionParams")) {
        linkObjectBuilder.setAndroidExecutionParams("androidExecutionParams");
      }

      if (object.has("iosExecutionParams")) {
        linkObjectBuilder.setIosExecutionParams("iosExecutionParams");
      }
    } catch (Exception e) {
      return null;
    }

    return linkObjectBuilder.build();
  }

  private SocialObject getSocialObject(JSONObject object) {
    if (object == null) {
      return null;
    }

    SocialObject.Builder socialObjectBuilder = new SocialObject.Builder();

    try {
      if (object.has("likeCount")) {
        socialObjectBuilder.setLikeCount(object.getInt("likeCount"));
      }

      if (object.has("viewCount")) {
        socialObjectBuilder.setViewCount(object.getInt("viewCount"));
      }

      if (object.has("sharedCount")) {
        socialObjectBuilder.setSharedCount(object.getInt("sharedCount"));
      }

      if (object.has("commentCount")) {
        socialObjectBuilder.setCommentCount(object.getInt("commentCount"));
      }

      if (object.has("subscriberCount")) {
        socialObjectBuilder.setSubscriberCount(object.getInt("subscriberCount"));
      }
    } catch (Exception e) {
      return null;
    }

    return socialObjectBuilder.build();
  }

  private ButtonObject getButtonObject(JSONObject object) {
    if (object == null) {
      return null;
    }

    ButtonObject buttonObject;

    try {
      LinkObject linkObject = getLinkObject(object.getJSONObject("link"));

      if (!object.has("title") || linkObject == null) {
        return null;
      }

      buttonObject = new ButtonObject(object.getString("title"), linkObject);
    } catch (Exception e) {
      return null;
    }

    return buttonObject;
  }

  private ContentObject getContentObject(JSONObject object) {
    if (object == null) {
      return null;
    }

    ContentObject.Builder contentObjectBuilder;

    try {
      LinkObject linkObject = getLinkObject(object.getJSONObject("link"));

      if (!object.has("title") || linkObject == null || !object.has("imageURL")) {
        return null;
      }

      contentObjectBuilder = new ContentObject.Builder(object.getString("title"), object.getString("imageURL"), linkObject);

      if (object.has("desc")) {
        contentObjectBuilder.setDescrption(object.getString("desc"));
      }

      if (object.has("imageWidth")) {
        contentObjectBuilder.setImageWidth(object.getInt("imageWidth"));
      }

      if (object.has("imageHeight")) {
        contentObjectBuilder.setImageHeight(object.getInt("imageHeight"));
      }
    } catch (Exception e) {
      return null;
    }

    return contentObjectBuilder.build();
  }

  private class SessionCallback implements ISessionCallback {
    private final CallbackContext callbackContext;

    public SessionCallback (CallbackContext callbackContext) {
      this.callbackContext = callbackContext;
    }

    @Override
    public void onSessionOpened() {
      requestMe(callbackContext);
    }

    @Override
    public void onSessionOpenFailed(KakaoException exception) {
      callbackContext.error(exception.toString());
    }
  }

  private static class KakaoSDKAdapter extends KakaoAdapter {
    @Override
    public ISessionConfig getSessionConfig() {
      return new ISessionConfig() {
        @Override
        public AuthType[] getAuthTypes() {
          return new AuthType[]{AuthType.KAKAO_TALK_ONLY};
        }

        @Override
        public boolean isUsingWebviewTimer() {
          return false;
        }

        @Override
        public boolean isSecureMode() {
          return false;
        }

        @Override
        public ApprovalType getApprovalType() {
          return ApprovalType.INDIVIDUAL;
        }

        @Override
        public boolean isSaveFormData() {
          return true;
        }
      };
    }

    @Override
    public IApplicationConfig getApplicationConfig() {
      return () -> KakaoCordovaSDK.getCurrentActivity().getApplicationContext();
    }
  }

  private void logout(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
      Session.getCurrentSession().addCallback(new SessionCallback(callbackContext));
      UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
        @Override
        public void onCompleteLogout() {
          Log.i(LOG_TAG, "로그아웃 완료");
          callbackContext.success();
        }
      });
    });
  }

  private void requestMe(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
      Session.getCurrentSession().addCallback(new SessionCallback(callbackContext));
      UserManagement.getInstance().me(new KakaoMeV2ResponseCallback(callbackContext));
    });
  }

  private void unlinkApp(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
      Session.getCurrentSession().addCallback(new SessionCallback(callbackContext));

      UserManagement.getInstance().requestUnlink(new UnLinkResponseCallback() {
        @Override
        public void onNotSignedUp() {
          callbackContext.error("카카오 계정에 연결되있지 않아요");
        }

        @Override
        public void onSuccess(Long userId) {
          callbackContext.success(Long.toString(userId));
        }

        @Override
        public void onFailure(ErrorResult errorResult) {
          Log.e(LOG_TAG, "연결 끊기 실패: " + errorResult);
          callbackContext.error((errorResult.getErrorMessage()));

        }

        @Override
        public void onSessionClosed(ErrorResult errorResult) {
          Log.e(LOG_TAG, "세션이 닫혀 있음: " + errorResult);
          callbackContext.error((errorResult.getErrorMessage()));
        }
      });
    });
  }

  private void addButtonsArray(JSONObject object, Object template) {
    if (object == null) {
      return;
    }

    try {
      if (!object.has("buttons")) {
        return;
      }

      JSONArray buttons = new JSONArray(object.getString("buttons"));

      if (buttons.length() < 1) {
        return;
      }

      for (int i = 0; i < buttons.length(); i++) {
        ButtonObject buttonObject = getButtonObject(buttons.getJSONObject(i));

        if (buttonObject == null) {
          continue;
        }

        ((FeedTemplate.Builder) template).addButton(buttonObject);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void getAccessToken(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
      Session.getCurrentSession().addCallback(new SessionCallback(callbackContext));

      String accessToken = Session.getCurrentSession().getTokenInfo().getAccessToken();

      if (accessToken == null || accessToken.equalsIgnoreCase("")) {
        callbackContext.error("");
      } else {
        callbackContext.success(accessToken);
      }
    });
  }

  private static class KakaoMeV2ResponseCallback extends MeV2ResponseCallback {
    private final CallbackContext callbackContext;

    public KakaoMeV2ResponseCallback(final CallbackContext callbackContext) {
      this.callbackContext = callbackContext;
    }

    @Override
    public void onSuccess(MeV2Response response) {
      try {
        JSONObject sendResult = new JSONObject();
        JSONObject receiveObject = new JSONObject(response.toString());
        JSONObject me = new JSONObject(receiveObject.getString("kakao_account"));
        String accessToken = Session.getCurrentSession().getTokenInfo().getAccessToken();

        sendResult.put("email", me.get("email"));
        sendResult.put("id", receiveObject.get("id"));
        sendResult.put("has_email", me.get("has_email"));

        Log.v(LOG_TAG, "kakao response: " + receiveObject);

        sendResult.put("accessToken", accessToken);

        callbackContext.success(sendResult);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onFailure(ErrorResult errorResult) {
      Log.e(LOG_TAG, "사용자 정보 요청 실패: " + errorResult);

      callbackContext.error(errorResult.toString());
    }

    @Override
    public void onSessionClosed(ErrorResult errorResult) {
      Log.e(LOG_TAG, "세션이 닫혀 있음: " + errorResult);

      callbackContext.error(errorResult.toString());
      Session.getCurrentSession().checkAndImplicitOpen();
    }
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, intent)) {
      return;
    }

    super.onActivityResult(requestCode, resultCode, intent);
  }

  private void sendLinkFeed(JSONArray options, final CallbackContext callbackContext) {
    try {
      SocialObject socialObject = null;
      JSONObject object = options.getJSONObject(0);

      if (object == null) {
        callbackContext.error("feed template is null");
        return;
      }

      if (!object.has("content")) {
        callbackContext.error("content is null");
        return;
      }

      ContentObject contentObject = getContentObject(object.getJSONObject("content"));

      if (contentObject == null) {
        callbackContext.error("Either Content or Content.title/link/imageURL is null");
        return;
      }

      FeedTemplate.Builder feedTemplateBuilder = new FeedTemplate.Builder(contentObject);

      if (object.has("social")) {
        socialObject = getSocialObject(object.getJSONObject("social"));
      }

      if (socialObject != null) {
        feedTemplateBuilder.setSocial(socialObject);
      }

      addButtonsArray(object, feedTemplateBuilder);

      KakaoLinkService.getInstance().sendDefault(currentActivity, feedTemplateBuilder.build(), new KakaoLinkResponseCallback(callbackContext));
    } catch (JSONException e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
  }

  private void sendLinkCustom(JSONArray options, final CallbackContext callbackContext) {
    String templateId;
    Map<String, String> templateArgs = new HashMap<>();
    Map<String, String> serverCallbackArgs = new HashMap<>();

    try {
      final JSONObject jsonObject = options.getJSONObject(0);

      if (!jsonObject.has("templateId")) {
        callbackContext.error("templateId is required");
        return;
      }

      templateId = jsonObject.getString("templateId");

      if (jsonObject.has("params")) {
        JSONObject params = new JSONObject(jsonObject.getString("params"));

        Iterator<?> keys = params.keys();

        while (keys.hasNext()) {
          String key = (String) keys.next();
          String value = params.getString(key);
          serverCallbackArgs.put(key, value);
        }
      }

      if (jsonObject.has("arguments")) {
        JSONObject arguments = new JSONObject(jsonObject.getString("arguments"));

        Iterator<?> keys = arguments.keys();

        while (keys.hasNext()) {
          String key = (String) keys.next();
          String value = arguments.getString(key);
          templateArgs.put(key, value);
        }
      }

      KakaoLinkService.getInstance().sendCustom(currentActivity, templateId, templateArgs, serverCallbackArgs, new KakaoLinkResponseCallback(callbackContext));
    } catch (Exception e) {
      e.printStackTrace();
      callbackContext.error((e.getMessage()));
    }
  }

  private static class KakaoLinkResponseCallback extends ResponseCallback<KakaoLinkResponse> {
    private final CallbackContext callbackContext;

    public KakaoLinkResponseCallback(final CallbackContext callbackContext) {
      this.callbackContext = callbackContext;
    }

    @Override
    public void onFailure(ErrorResult errorResult) {
      callbackContext.error(errorResult.getErrorMessage());
    }

    @Override
    public void onSuccess(KakaoLinkResponse result) {
      callbackContext.success();
    }
  }
}