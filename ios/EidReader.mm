//
//  EidReader.mm
//  react-native-eid-reader
//
//  Codegen-driven TurboModule (New Architecture). The public surface is
//  declared in `src/NativeEIdReader.ts`; the Codegen step generates the
//  `NativeEIdReaderSpec` protocol that this class conforms to. All real
//  work is delegated to the `EIdReaderImpl` Swift class.
//
//  Previously this file used the legacy `RCT_EXTERN_MODULE` / `RCT_EXTERN_METHOD`
//  macros. Under the New Architecture those are replaced by ordinary
//  Objective-C method implementations that match the signatures in the
//  generated `NativeEIdReaderSpec` protocol.
//

#import "EidReader.h"

// Swift-generated header exposing the `EIdReaderImpl` class to Objective-C.
// CocoaPods writes it to `${DERIVED_FILE_DIR}` during this target's Swift
// compilation; that directory is on the default header search path, so the
// quoted import resolves. Do NOT wrap this in `__has_include(...)` — clang's
// `__has_include` does not consult `DERIVED_FILE_DIR` even though `#import`
// does, so the guards silently fall through and leave `EIdReaderImpl`
// undeclared.
#import "react_native_eid_reader-Swift.h"

@interface EIdReader ()
@property (nonatomic, strong) EIdReaderImpl *impl;
@end

@implementation EIdReader

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

- (instancetype)init
{
  if ((self = [super init])) {
    _impl = [[EIdReaderImpl alloc] init];
  }
  return self;
}

#pragma mark - NativeEIdReaderSpec

- (void)isNfcSupported:(RCTPromiseResolveBlock)resolve
                reject:(RCTPromiseRejectBlock)reject
{
  [_impl isNfcSupported:resolve withRejecter:reject];
}

- (void)isNfcEnabled:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  [_impl isNfcEnabled:resolve withRejecter:reject];
}

- (void)openNfcSettings:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject
{
  [_impl openNfcSettings:resolve withRejecter:reject];
}

- (void)startReading:(JS::NativeEIdReader::StartReadingParams &)params
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  // The generated spec materialises the typed-object parameter as a C++ value
  // type. Convert it into an `NSDictionary *` matching the shape the Swift
  // implementation already expects, so the Swift code stays unchanged.
  NSMutableDictionary *dict = [NSMutableDictionary new];

  // `mrzInfo` and `can` are both optional in the JS spec; the caller must
  // supply exactly one of them. The Swift side validates this.
  if (auto mrz = params.mrzInfo()) {
    dict[@"mrzInfo"] = @{
      @"expirationDate": @(mrz->expirationDate().UTF8String ?: ""),
      @"birthDate":      @(mrz->birthDate().UTF8String ?: ""),
      @"documentNumber": @(mrz->documentNumber().UTF8String ?: ""),
    };
  }
  if (NSString *can = params.can()) {
    dict[@"can"] = can;
  }

  if (auto includeImages = params.includeImages()) {
    dict[@"includeImages"] = @(*includeImages);
  }
  if (auto includeRawData = params.includeRawData()) {
    dict[@"includeRawData"] = @(*includeRawData);
  }
  if (auto labels = params.labels()) {
    NSMutableDictionary *labelsDict = [NSMutableDictionary new];
    #define COPY_LABEL(key) \
      if (auto v = labels->key()) { labelsDict[@#key] = @(v.UTF8String ?: ""); }
    COPY_LABEL(title)
    COPY_LABEL(cancelButton)
    COPY_LABEL(requestPresentPassport)
    COPY_LABEL(authenticatingWithPassport)
    COPY_LABEL(reading)
    COPY_LABEL(activeAuthentication)
    COPY_LABEL(successfulRead)
    COPY_LABEL(tagNotValid)
    COPY_LABEL(moreThanOneTagFound)
    COPY_LABEL(invalidMRZKey)
    COPY_LABEL(error)
    #undef COPY_LABEL
    dict[@"labels"] = labelsDict;
  }

  [_impl startReading:dict withResolver:resolve withRejecter:reject];
}

- (void)stopReading
{
  [_impl stopReading];
}

- (NSString *)imageDataUrlToJpegDataUrl:(NSString *)dataUrl
{
  return [_impl imageDataUrlToJpegDataUrl:dataUrl] ?: @"";
}

#pragma mark - NativeEventEmitter lifecycle

- (void)addListener:(NSString *)eventName
{
  // no-op: events are not emitted from the iOS side of this module.
}

- (void)removeListeners:(double)count
{
  // no-op
}

#pragma mark - TurboModule registration

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeEIdReaderSpecJSI>(params);
}

@end
