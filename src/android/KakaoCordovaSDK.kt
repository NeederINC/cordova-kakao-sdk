package com.needer.plugin.kakao

import android.content.ActivityNotFoundException
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.user.UserApiClient
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaArgs
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONObject


class KakaoCordovaSDK : CordovaPlugin() {
  override fun pluginInitialize() {
    super.pluginInitialize()

    try {
      val e: String = super.cordova.context.packageName
      val ai: ApplicationInfo = super.cordova.context.packageManager.getApplicationInfo(e, PackageManager.GET_META_DATA)
      val bundle: Bundle = ai.metaData
      var apiKey = bundle.getString("com.kakao.sdk.AppKey")

      if (apiKey != null) {
        KakaoSdk.init(super.cordova.context, apiKey)
      }
    } catch (var6: Exception) {
      Log.d("KAKAO_SDK", "Caught non-fatal exception while retrieving apiKey: $var6")
    }
  }

  override fun execute(action: String?, args: CordovaArgs?, callbackContext: CallbackContext?): Boolean {
    when (action) {
      "login" -> {
        if (callbackContext != null) {
          this.login(callbackContext)
        }
      }

      "sendLinkFeed" -> {
        val arg = args?.get(0)

        if (arg != null && arg is JSONObject && callbackContext != null) {
          this.sendLinkFeed(arg, callbackContext)
        }
      }
    }

    return true
  }

  private fun sendLinkFeed(arg: JSONObject, callbackContext: CallbackContext) {
    val template = FeedTemplate(content = this.createContent(arg.getJSONObject("content")), social = null, buttons = this.createButtons(arg.getJSONArray("buttons")))
    val serverCallbackArgs = HashMap<String, String>()
    val serverCallbackArgsJSON = arg.getJSONObject("content").getJSONObject("serverCallbackArgs")
    val it = serverCallbackArgsJSON.keys()

    while (it.hasNext()) {
      val key = it.next()
      val keyValue = serverCallbackArgsJSON.get(key)
      serverCallbackArgs[key] = keyValue.toString()
    }

    if (ShareClient.instance.isKakaoTalkSharingAvailable(this.cordova.context)) {
      // 앱 띄우고 공유
      ShareClient.instance.shareDefault(this.cordova.context, template, serverCallbackArgs) { sharingResult, error ->
        if (error != null) {
          callbackContext.error(error.localizedMessage)
          return@shareDefault
        }

        if (sharingResult != null) {
          // 결과 버림
          this.cordova.activity.startActivity(sharingResult.intent)
          callbackContext.success()
        } else {
          callbackContext.error("Cannot read share info")
        }
      }
    } else {
      // 웹 띄우고 공유
      val sharerUrl = WebSharerClient.instance.makeDefaultUrl(template, serverCallbackArgs)
      try {
        this.cordova.activity.let {
          KakaoCustomTabsClient.openWithDefault(it, sharerUrl)
        }
      } catch (e: UnsupportedOperationException) {
        try {
          this.cordova.activity.let {
            KakaoCustomTabsClient.open(it, sharerUrl)
          }
        } catch (e: ActivityNotFoundException) {
          callbackContext.error("Cannot open share link")
        }
      }
    }
  }

  private fun createButton(obj: JSONObject): Button {
    val title: String = obj.getString("title")
    val link: Link = createLink(obj.getJSONObject(("link")))
    return Button(title, link)
  }

  private fun createButtons(obj: JSONArray): List<Button> {
    val buttons = mutableListOf<Button>()

    for (i in 0 until obj.length()) {
      buttons.add(this.createButton(obj.getJSONObject(i)))
    }

    return buttons
  }

  private fun createContent(obj: JSONObject): Content {
    val title: String = obj.getString("title")
    val url: String = obj.getString("imageURL")
    val link: Link = createLink(obj.getJSONObject("link"))
    val desc: String? = obj.getString("desc")
    return Content(title, description = desc, imageUrl = url, link = link)
  }

  private fun createLink(obj: JSONObject): Link {
    val webURL: String? = obj.getString("webURL")
    val mobileWebURL: String? = obj.getString("mobileWebURL")
    return Link(webUrl = webURL, mobileWebUrl = mobileWebURL)
  }

  private fun login(callbackContext: CallbackContext) {
    if (UserApiClient.instance.isKakaoTalkLoginAvailable(this.cordova.context)) {
      this.cordova.activity.let {
        UserApiClient.instance.loginWithKakaoTalk(it) { token, error: Throwable? ->
          if (error != null) {
            if (error is AuthError && error.statusCode == 302) {
              this.loginWithBrowser(callbackContext)
              return@loginWithKakaoTalk
            }
            callbackContext.error(error.localizedMessage)
            return@loginWithKakaoTalk
          }
          if (token != null) {
            this.loginSuccessCallback(token.accessToken, callbackContext)
            return@loginWithKakaoTalk
          } else {
            callbackContext.error("Invalid Credential")
            return@loginWithKakaoTalk
          }
        }
      }
    } else {
      this.loginWithBrowser(callbackContext)
    }
  }

  private fun loginWithBrowser(callbackContext: CallbackContext) {
    UserApiClient.instance.loginWithKakaoAccount(this.cordova.context) { token, error ->
      if (error != null) {
        callbackContext.error(error.localizedMessage)
        return@loginWithKakaoAccount
      }

      if (token != null) {
        this.loginSuccessCallback(token.accessToken, callbackContext)
        return@loginWithKakaoAccount
      } else {
        callbackContext.error("Invalid Credential")
        return@loginWithKakaoAccount
      }
    }
  }

  private fun loginSuccessCallback(accessToken: String, callbackContext: CallbackContext) {
    UserApiClient.instance.me { user, error ->
      if (error != null) {
        callbackContext.error(error.localizedMessage)
        return@me
      }

      if (user != null) {
        val json = JSONObject()
        json.put("id", user.id)
        json.put("email", user.kakaoAccount?.email)
        json.put("result", true)
        json.put("accessToken", accessToken)
        callbackContext.success(json)
        return@me
      } else {
        callbackContext.error("User not found")
        return@me
      }
    }
  }
}