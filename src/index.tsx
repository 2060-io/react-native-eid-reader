import { DeviceEventEmitter } from 'react-native';

import EIdReaderNativeModule from './NativeEIdReader';

enum EIdReaderEvent {
  TAG_DISCOVERED = 'onTagDiscovered',
  NFC_STATE_CHANGED = 'onNfcStateChanged',
}

export type StartReadingParams = {
  mrzInfo: {
    expirationDate: string;
    birthDate: string;
    documentNumber: string;
  };
  includeImages?: boolean; // default: false
  includeRawData?: boolean; // default: false
  labels?: {
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
};

export type EidReadStatus = 'OK' | 'Error' | 'Canceled';

export type EIdReadResult = {
  status: EidReadStatus;
  data: {
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
    originalFacePhoto?: string; // base64
  };
  dataGroupsBase64: Record<string, string>;
};

export default class EIdReader {
  static async startReading(
    params: StartReadingParams
  ): Promise<EIdReadResult> {
    // Codegen only supports `string` (not string-literal unions) as the
    // TurboModule return type for `status`, so we narrow at the JS boundary.
    const result = await EIdReaderNativeModule.startReading(params);
    return result as EIdReadResult;
  }

  static stopReading() {
    EIdReaderNativeModule.stopReading();
  }

  static addOnTagDiscoveredListener(callback: () => void) {
    this.addListener(EIdReaderEvent.TAG_DISCOVERED, callback);
  }

  static addOnNfcStateChangedListener(callback: (state: 'off' | 'on') => void) {
    this.addListener(EIdReaderEvent.NFC_STATE_CHANGED, callback);
  }

  static isNfcEnabled(): Promise<boolean> {
    return EIdReaderNativeModule.isNfcEnabled();
  }

  static isNfcSupported(): Promise<boolean> {
    return EIdReaderNativeModule.isNfcSupported();
  }

  static openNfcSettings(): Promise<boolean> {
    return EIdReaderNativeModule.openNfcSettings();
  }

  static imageDataUrlToJpegDataUrl(dataUrl: string): string {
    return EIdReaderNativeModule.imageDataUrlToJpegDataUrl(dataUrl);
  }

  private static addListener(
    event: EIdReaderEvent,
    callback: (data: any) => void
  ) {
    DeviceEventEmitter.addListener(event, callback);
  }

  static removeListeners() {
    DeviceEventEmitter.removeAllListeners(EIdReaderEvent.TAG_DISCOVERED);
    DeviceEventEmitter.removeAllListeners(EIdReaderEvent.NFC_STATE_CHANGED);
  }
}
