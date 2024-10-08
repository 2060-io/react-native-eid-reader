import { NativeModules, DeviceEventEmitter, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@2060.io/react-native-eid-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const EidReaderNativeModule = NativeModules.EidReader
  ? NativeModules.EidReader
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

enum NfcPassportReaderEvent {
  TAG_DISCOVERED = 'onTagDiscovered',
  NFC_STATE_CHANGED = 'onNfcStateChanged',
}

export type StartReadingParams = {
  mrz: string;
  includeImages?: boolean; // default: false
};

export type NfcResult = {
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

export default class NfcPassportReader {
  static startReading(params: StartReadingParams): Promise<NfcResult> {
    return EidReaderNativeModule.startReading(params);
  }

  static stopReading() {
    EidReaderNativeModule.stopReading();
  }

  static addOnTagDiscoveredListener(callback: () => void) {
    this.addListener(NfcPassportReaderEvent.TAG_DISCOVERED, callback);
  }

  static addOnNfcStateChangedListener(callback: (state: 'off' | 'on') => void) {
    this.addListener(NfcPassportReaderEvent.NFC_STATE_CHANGED, callback);
  }

  static isNfcEnabled(): Promise<boolean> {
    return EidReaderNativeModule.isNfcEnabled();
  }

  static isNfcSupported(): Promise<boolean> {
    return EidReaderNativeModule.isNfcSupported();
  }

  static openNfcSettings(): Promise<boolean> {
    return EidReaderNativeModule.openNfcSettings();
  }

  private static addListener(
    event: NfcPassportReaderEvent,
    callback: (data: any) => void
  ) {
    DeviceEventEmitter.addListener(event, callback);
  }

  static removeListeners() {
    DeviceEventEmitter.removeAllListeners(
      NfcPassportReaderEvent.TAG_DISCOVERED
    );
    DeviceEventEmitter.removeAllListeners(
      NfcPassportReaderEvent.NFC_STATE_CHANGED
    );
  }
}
