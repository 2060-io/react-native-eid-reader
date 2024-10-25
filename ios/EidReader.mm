#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(EIdReader, RCTEventEmitter)

RCT_EXTERN_METHOD(isNfcSupported:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(isNfcEnabled:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(openNfcSettings:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(startReading:(NSDictionary *)params
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject);

RCT_EXTERN__BLOCKING_SYNCHRONOUS_METHOD(stopReading);

RCT_EXTERN_METHOD(imageDataUrlToJpegDataUrl:(NSString)dataUrl
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject);

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
