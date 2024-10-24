import { NativeModules, DeviceEventEmitter, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@2060.io/react-native-eid-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const EIdReaderNativeModule = NativeModules.EIdReader
  ? NativeModules.EIdReader
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

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
  static startReading(params: StartReadingParams): Promise<EIdReadResult> {
    return EIdReaderNativeModule.startReading(params);
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

  static imageDataUrlToJpegDataUrl(dataUrl: string): Promise<string> {
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
