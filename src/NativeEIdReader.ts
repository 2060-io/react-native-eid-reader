import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// -----------------------------------------------------------------------------
// Codegen-compatible shapes.
//
// RN Codegen does not generate custom Java POJOs for nested objects; on Android
// typed objects are marshalled as `ReadableMap`, and on iOS as `NSDictionary`.
// Keeping the shapes here gives JS callers strong types without any codegen
// cost.
// -----------------------------------------------------------------------------

export type StartReadingMrzInfo = {
  expirationDate: string;
  birthDate: string;
  documentNumber: string;
};

export type StartReadingLabels = {
  title?: string;
  cancelButton?: string;
  requestPresentPassport?: string;
  authenticatingWithPassport?: string;
  reading?: string;
  activeAuthentication?: string;
  successfulRead?: string;
  tagNotValid?: string;
  moreThanOneTagFound?: string;
  invalidMRZKey?: string;
  error?: string;
};

export type StartReadingParams = {
  mrzInfo: StartReadingMrzInfo;
  includeImages?: boolean;
  includeRawData?: boolean;
  labels?: StartReadingLabels;
};

export type EIdData = {
  birthDate?: string;
  placeOfBirth?: string;
  documentNo?: string;
  expiryDate?: string;
  firstName?: string;
  gender?: string;
  identityNo?: string;
  lastName?: string;
  mrz?: string;
  nationality?: string;
  originalFacePhoto?: string;
};

export type EIdReadResult = {
  status: string; // 'OK' | 'Error' | 'Canceled'
  data: EIdData;
  dataGroupsBase64: { [key: string]: string };
};

export interface Spec extends TurboModule {
  startReading(params: StartReadingParams): Promise<EIdReadResult>;
  stopReading(): void;
  isNfcEnabled(): Promise<boolean>;
  isNfcSupported(): Promise<boolean>;
  openNfcSettings(): Promise<boolean>;
  imageDataUrlToJpegDataUrl(dataUrl: string): string;

  // NativeEventEmitter lifecycle — declared so this module can act as a
  // TurboModule-backed event emitter. Events are emitted from the native side
  // through `RCTDeviceEventEmitter` (Android) and are consumed via
  // `DeviceEventEmitter` on the JS side. These methods are intentional no-ops
  // on the native side.
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('EIdReader');
