#import "KakaoCordovaSDK.h"
#import <Cordova/CDVPlugin.h>
#import <objc/runtime.h>

@implementation KakaoCordovaSDK

- (void) pluginInitialize {
  NSLog(@"Start KaKao plugin");
    
  [[NSNotificationCenter defaultCenter] addObserver:self
                                        selector:@selector(applicationDidBecomeActive)
                                        name:UIApplicationDidBecomeActiveNotification
                                        object:nil];
}

- (void) applicationDidBecomeActive {
  [KOSession handleDidBecomeActive];
}

- (void) login:(CDVInvokedUrlCommand*) command {
  [[KOSession sharedSession] close];

  [[KOSession sharedSession] openWithCompletionHandler:^(NSError *error) {
    if (error) {
      NSLog(@"login failed. - error: %@", error);

      CDVPluginResult* pluginResult = nil;

      NSDictionary *errorObject = @{
        @"result": @"false",
        @"message": error.userInfo[@"NSLocalizedFailureReason"]
      };

      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorObject];

      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
      [KOSessionTask userMeTaskWithCompletion:^(NSError * _Nullable error, KOUserMe * _Nullable me) {
        CDVPluginResult* pluginResult = nil;

        if (error) {
          NSLog(@"login failed. - error: %@", error);

          NSDictionary *errorObject = @{
            @"result": @"false",
            @"message": error.userInfo[@"NSLocalizedDescription"]
          };

          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorObject];

          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } else {
          NSDictionary *meObject = @{
            @"id": me.ID,
            @"result": @"true",
            @"email": me.account.email,
            @"accessToken": [KOSession sharedSession].token.accessToken
          };

          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:meObject];

          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
      }];
    }
  } authType:(KOAuthType)KOAuthTypeTalk, nil];
}

- (void) logout:(CDVInvokedUrlCommand*) command {
  [[KOSession sharedSession] logoutAndCloseWithCompletionHandler:^(BOOL success, NSError *error) {
    CDVPluginResult* pluginResult = nil;

    if (success) {
      NSLog(@"Successful logout.");
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
      NSLog(@"failed to logout.");

      NSDictionary *errorObject = @{
        @"result": @"false",
        @"message": error.userInfo[@"NSLocalizedDescription"]
      };

      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorObject];

      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }];
}

- (void) unlinkApp:(CDVInvokedUrlCommand*) command {
  [KOSessionTask unlinkTaskWithCompletionHandler:^(BOOL success, NSError *error) {
    CDVPluginResult* pluginResult = nil;

    if (success) {
      NSLog(@"Successful unlink.");

      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];

      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
      NSLog(@"failed to unlink.");

      NSDictionary *errorObject = @{
        @"result": @"false",
        @"message": error.userInfo[@"NSLocalizedDescription"]
      };

      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorObject];

      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }];
}

- (void) sendLinkFeed:(CDVInvokedUrlCommand*) command {
  NSMutableDictionary *options = [[command.arguments lastObject] mutableCopy];

  NSLog(@"%@", options);

  // feed template
  KMTTemplate *template = [KMTFeedTemplate feedTemplateWithBuilderBlock:^(KMTFeedTemplateBuilder * _Nonnull feedTemplateBuilder) {
    CDVPluginResult* pluginResult = nil;
    KMTContentObject* feedContentObject = [self getKMTContentObject:options[@"content"]];

    if (feedContentObject == NULL) {
      NSString *errorMessage = @"title/link/imageURL를 확인해주세요";
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];

      return [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

    feedTemplateBuilder.content = feedContentObject;

    // social
    KMTSocialObject* feedSocialObject = [self getKMTSocialObject:options[@"social"]];

    if (feedSocialObject != NULL) {
      feedTemplateBuilder.social = feedSocialObject;
    }

    // buttons
    [self addButtonsArray:options[@"buttons"] templateBuilder:feedTemplateBuilder];
  }];

  // 카카오링크 실행
  [self sendDefaultWithTemplate:template params:options[@"params"] command:command];
}

- (void) getAccessToken:(CDVInvokedUrlCommand*) command {
  [KOSessionTask accessTokenInfoTaskWithCompletionHandler:^(KOAccessTokenInfo *accessTokenInfo, NSError *error) {
    NSLog(@"getAccessToken");

    CDVPluginResult* pluginResult = nil;

    if (error) {
      NSString *errorMessage = @"세션이 만료되었습니다";

      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];

      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
      NSLog(@"success request - access token info:  %@", accessTokenInfo);

      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];

      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }];
}

- (void) sendLinkCustom:(CDVInvokedUrlCommand*) command {
  NSMutableDictionary *options = [[command.arguments lastObject] mutableCopy];

  NSLog(@"%@", options);

  CDVPluginResult* pluginResult = nil;

  if (options[@"params"] == NULL) {
    NSString *errorMessage = @"params를 확인해주세요";
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];

    return [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  } else if (options[@"templateId"] == NULL) {
    NSString *errorMessage = @"templateId를 확인해주세요";
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];

    return [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }

  NSString *templateId = options[@"templateId"];
  NSDictionary *serverCallbackArgs = options[@"params"];
  NSMutableDictionary* templateArgs = options[@"arguments"];

  // 카카오링크 실행
  [[KLKTalkLinkCenter sharedCenter] sendCustomWithTemplateId:templateId
                                    templateArgs:templateArgs
                                    serverCallbackArgs:serverCallbackArgs
                                    success:^(NSDictionary<NSString *,NSString *> * _Nullable warningMsg, NSDictionary<NSString *,NSString *> * _Nullable argumentMsg) {
    // 성공
    NSLog(@"warning message: %@", warningMsg);
    NSLog(@"argument message: %@", argumentMsg);
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  } failure:^(NSError * _Nonnull error) {
    NSLog(@"error message: %@", error);
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (KMTLinkObject *) getKMTLinkObject:(NSDictionary *) object {
  if (object == NULL) {
    return NULL;
  }

  return [KMTLinkObject linkObjectWithBuilderBlock:^(KMTLinkBuilder * _Nonnull linkBuilder) {
    NSString *webURL = object[@"webURL"];
    NSString *mobileWebURL = object[@"mobileWebURL"];

    if (webURL != NULL) {
      linkBuilder.webURL = [NSURL URLWithString:webURL];
    }

    if (mobileWebURL != NULL) {
      linkBuilder.mobileWebURL = [NSURL URLWithString:mobileWebURL];
    }
  }];
}

- (KMTButtonObject *) getKMTButtonObject:(NSDictionary *) object {
  if (object == NULL) {
    return NULL;
  }

  return [KMTButtonObject buttonObjectWithBuilderBlock:^(KMTButtonBuilder * _Nonnull buttonBuilder) {
    buttonBuilder.title = object[@"title"];

    KMTLinkObject* linkObject = [self getKMTLinkObject:object[@"link"]];

    if (linkObject != NULL) {
      buttonBuilder.link = linkObject;
    }
  }];
}

- (KMTSocialObject *) getKMTSocialObject:(NSDictionary *) object {
  if (object == NULL) {
    return NULL;
  }

  return [KMTSocialObject socialObjectWithBuilderBlock:^(KMTSocialBuilder * _Nonnull socialBuilder) {
    NSString *likeCount = object[@"likeCount"];
    NSString *viewCount = object[@"viewCount"];
    NSString *sharedCount = object[@"sharedCount"];
    NSString *commnentCount = object[@"commnentCount"];

    if (likeCount != NULL) {
      socialBuilder.likeCount = [NSNumber numberWithInt:[likeCount intValue]];
    }

    if (commnentCount != NULL) {
      socialBuilder.commnentCount = [NSNumber numberWithInt:[commnentCount intValue]];
    }

    if (sharedCount != NULL) {
      socialBuilder.sharedCount = [NSNumber numberWithInt:[sharedCount intValue]];
    }

    if (viewCount != NULL) {
      socialBuilder.viewCount = [NSNumber numberWithInt:[viewCount intValue]];
    }
  }];
}

- (KMTContentObject *) getKMTContentObject:(NSDictionary *) object {
  if (object == NULL) {
    return NULL;
  }

  KMTLinkObject* linkObject = [self getKMTLinkObject:object[@"link"]];

  if(object[@"title"] == NULL || linkObject == NULL || object[@"imageURL"] == NULL){
    return NULL;
  }

  return [KMTContentObject contentObjectWithBuilderBlock:^(KMTContentBuilder * _Nonnull contentBuilder) {
    contentBuilder.link = linkObject;
    contentBuilder.title = object[@"title"];
    contentBuilder.imageURL = [NSURL URLWithString:object[@"imageURL"]];

    NSString *desc = object[@"desc"];

    if (desc != NULL) {
      contentBuilder.desc = desc;
    }
  }];
}

- (void) addButtonsArray:(NSArray *) object templateBuilder:(NSObject *) templateBuilder {
  if (object == NULL) {
    return;
  }

  NSArray* buttons = object;

  if ([buttons count] < 1) {
    return;
  }
    
  for (int i = 0; i < [buttons count]; i++) {
    KMTButtonObject* feedButtonObject = [self getKMTButtonObject:buttons[i]];

    if (feedButtonObject != NULL) {
      if ([templateBuilder isKindOfClass:[KMTFeedTemplateBuilder class]]) {
        [((KMTFeedTemplateBuilder*)templateBuilder) addButton: feedButtonObject];
      } else if ([templateBuilder isKindOfClass:[KMTTextTemplateBuilder class]]) {
        [((KMTTextTemplateBuilder*)templateBuilder) addButton: feedButtonObject];
      } else if ([templateBuilder isKindOfClass:[KMTListTemplateBuilder class]]) {
        [((KMTListTemplateBuilder*)templateBuilder) addButton: feedButtonObject];
      } else if ([templateBuilder isKindOfClass:[KMTCommerceTemplateBuilder class]]) {
        [((KMTCommerceTemplateBuilder*)templateBuilder) addButton: feedButtonObject];
      } else if ([templateBuilder isKindOfClass:[KMTLocationTemplateBuilder class]]) {
        [((KMTLocationTemplateBuilder*)templateBuilder) addButton: feedButtonObject];
      }
    }
  }
}

- (void) sendDefaultWithTemplate:(KMTTemplate*) template params:(NSDictionary*) params command:(CDVInvokedUrlCommand*) command {
  [[KLKTalkLinkCenter sharedCenter] sendDefaultWithTemplate:template
                                    serverCallbackArgs:params
                                    success:^(NSDictionary<NSString *,NSString *> * _Nullable warningMsg, NSDictionary<NSString *,NSString *> * _Nullable argumentMsg) {
    // 성공
    NSLog(@"warning message: %@", warningMsg);
    NSLog(@"argument message: %@", argumentMsg);
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  } failure:^(NSError * _Nonnull error) {
    NSLog(@"error: %@", error);
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@""];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

@end

#pragma mark - AppDelegate Overrides

@implementation AppDelegate (KakaoCordovaSDK)

void KMethodSwizzle(Class c, SEL originalSelector) {
  NSString *selectorString = NSStringFromSelector(originalSelector);

  SEL noopSelector = NSSelectorFromString([@"noop_kakao_" stringByAppendingString:selectorString]);
  SEL newSelector = NSSelectorFromString([@"swizzled_kakao_" stringByAppendingString:selectorString]);

  Method originalMethod, newMethod, noop;
  noop = class_getInstanceMethod(c, noopSelector);
  newMethod = class_getInstanceMethod(c, newSelector);
  originalMethod = class_getInstanceMethod(c, originalSelector);

  if (class_addMethod(c, originalSelector, method_getImplementation(newMethod), method_getTypeEncoding(newMethod))) {
    class_replaceMethod(c, newSelector, method_getImplementation(originalMethod) ?: method_getImplementation(noop), method_getTypeEncoding(originalMethod));
  } else {
    method_exchangeImplementations(originalMethod, newMethod);
  }
}

+ (void) load {
  KMethodSwizzle([self class], @selector(application:openURL:sourceApplication:annotation:));
}

// This method is a duplicate of the other openURL method below, except using the newer iOS (9) API.
- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url options:(NSDictionary<NSString *,id> *)options {
  if (!url) {
    return NO;
  }

  if ([KOSession isKakaoAccountLoginCallback:url]) {
    [KOSession handleOpenURL:url];
  }

  if ([[KLKTalkLinkCenter sharedCenter] isTalkLinkCallback:url]) {
    NSString *params = url.query;
    NSLog(@"%@", params);
  }

  NSLog(@"Kakao(ori) handle url: %@", url);

  // Call existing method
  return [self swizzled_kakao_application:application openURL:url sourceApplication:[options valueForKey:@"UIApplicationOpenURLOptionsSourceApplicationKey"] annotation:0x0];
}

- (BOOL)noop_kakao_application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
  return NO;
}

- (BOOL)swizzled_kakao_application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
  if (!url) {
    return NO;
  }

  if ([KOSession isKakaoAccountLoginCallback:url]) {
    [KOSession handleOpenURL:url];
  }

  if ([[KLKTalkLinkCenter sharedCenter] isTalkLinkCallback:url]) {
    NSString *params = url.query;
    NSLog(@"%@", params);
  }

  NSLog(@"Kakao(swizzle) handle url: %@", url);

  // Call existing method
  return [self swizzled_kakao_application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
}
@end
