
#import "AppDelegate.h"
#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>
#import <KakaoLink/KakaoLink.h>
#import <KakaoOpenSDK/KakaoOpenSDK.h>
#import <KakaoPlusFriend/KakaoPlusFriend.h>
#import <KakaoMessageTemplate/KakaoMessageTemplate.h>

@interface KakaoCordovaSDK : CDVPlugin <UIImagePickerControllerDelegate, UINavigationControllerDelegate, UIScrollViewDelegate>

- (void) login:(CDVInvokedUrlCommand*)command;
- (void) logout:(CDVInvokedUrlCommand*)command;
- (void) unlinkApp:(CDVInvokedUrlCommand*)command;
- (void) sendLinkFeed:(CDVInvokedUrlCommand*)command;
- (void) sendLinkCustom:(CDVInvokedUrlCommand*)command;
- (void) getAccessToken:(CDVInvokedUrlCommand*)command;
@end

