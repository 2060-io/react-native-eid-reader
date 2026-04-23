# react-native-eid-reader

[![npm version](https://img.shields.io/npm/v/@2060.io/react-native-eid-reader.svg)](https://www.npmjs.com/package/@2060.io/react-native-eid-reader)
[![license](https://img.shields.io/npm/l/@2060.io/react-native-eid-reader.svg)](./LICENSE)
[![platforms](https://img.shields.io/badge/platforms-android%20%7C%20ios-lightgrey.svg)](#requirements)

A React Native module to read electronic passports and ID cards over NFC, following the **ICAO Doc 9303** standard for Machine Readable Travel Documents (MRTDs).

Given the MRZ (Machine Readable Zone) information of a document — document number, date of birth and expiration date — or the Card Access Number (CAN) printed on the card, the library authenticates against the chip (PACE or BAC), reads the relevant data groups, and returns personal data and (optionally) the portrait image stored inside the document.

This module is used in production by [Hologram Messaging](https://hologram.zone) as part of its real-world identity verification flows.

## Features

- 📖 Reads **ICAO 9303** compliant electronic passports and national eID cards (TD1, TD2, TD3)
- 🔐 **PACE** (Password Authenticated Connection Establishment) with automatic fallback to **BAC** (Basic Access Control)
  - PACE-MRZ — derived from document number + DOB + expiry
  - PACE-CAN — 6-digit Card Access Number (required by some eIDs, e.g. the German Personalausweis)
- 🆔 Supports TD1 ID cards with extended (>9-char) document numbers (e.g. Moroccan CNIe)
- 👤 Returns parsed personal data from DG1 (MRZ) and portrait image from DG2
- 🗂️ Optional access to raw data groups (base64) for advanced use cases (PA, signature verification, …)
- 🖼️ Helper to convert the embedded JPEG2000 portrait to JPEG so it can be displayed in a standard `<Image />`
- 📡 NFC state & tag-discovered event listeners
- ⚙️ NFC capability checks and deep-link into system NFC settings (Android)
- 🤖 Android & 🍎 iOS support
- 🏗️ New Architecture (TurboModules) ready

## Requirements

| Platform | Minimum version | Notes                                                                                |
| -------- | --------------- | ------------------------------------------------------------------------------------ |
| Android  | API 21 (5.0)    | Device must have NFC hardware                                                        |
| iOS      | 16.0            | Physical device required; Simulator has no NFC. Requires the NFC capability & entitlement. iOS 16 is needed for the `.pace` polling option used to reliably detect post-2021 eIDs (e.g. French CNIe). |

## Installation

```sh
yarn add @2060.io/react-native-eid-reader
# or
npm install @2060.io/react-native-eid-reader
```

### iOS

After installing the package, run `pod install`:

```sh
cd ios && pod install
```

Add the NFC capability to your app:

1. In Xcode, enable **Near Field Communication Tag Reading** under *Signing & Capabilities*. This creates/updates your `*.entitlements` file with:

   ```xml
   <key>com.apple.developer.nfc.readersession.formats</key>
   <array>
     <string>TAG</string>
   </array>
   ```

2. Add the following keys to your `Info.plist`:

   ```xml
   <key>NFCReaderUsageDescription</key>
   <string>This app uses NFC to read your ID document</string>

   <key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
   <array>
     <string>A0000002471001</string>
     <string>A0000002472001</string>
     <string>00000000000000</string>
   </array>
   ```

### Android

No manual steps are required — the library's manifest already declares the NFC permission and autolinking takes care of the rest.

## Usage

```tsx
import EIdReader, {
  type EIdReadResult,
} from '@2060.io/react-native-eid-reader';

async function scan() {
  // NFC state & tag events (optional)
  EIdReader.addOnNfcStateChangedListener((state) => console.log('NFC', state));
  EIdReader.addOnTagDiscoveredListener(() => console.log('Tag discovered'));

  try {
    // Option A — MRZ-based authentication (BAC / PACE-MRZ)
    const result: EIdReadResult = await EIdReader.startReading({
      mrzInfo: {
        documentNumber: 'L726DHAM0',  // 9-char alphanumeric, or the full extended
                                      // document number for TD1 cards that have one
        birthDate: '751213',          // YYMMDD
        expirationDate: '341112',     // YYMMDD
      },
      includeImages: true,
      includeRawData: false,
    });

    // Option B — CAN-based authentication (PACE-CAN)
    //   Used by eIDs that do not accept BAC/PACE-MRZ. The CAN is a 6-digit
    //   code printed on the card (e.g. the German Personalausweis).
    // const result: EIdReadResult = await EIdReader.startReading({
    //   can: '123456',
    //   includeImages: true,
    // });

    if (result.status === 'OK') {
      console.log(result.data.firstName, result.data.lastName);

      if (result.data.originalFacePhoto) {
        // The portrait is JPEG2000; convert to JPEG so <Image/> can render it
        const jpegDataUrl = EIdReader.imageDataUrlToJpegDataUrl(
          `data:image/jp2;base64,${result.data.originalFacePhoto}`
        );
        // <Image source={{ uri: jpegDataUrl }} />
      }
    }
  } catch (e) {
    console.error(e);
  } finally {
    EIdReader.stopReading();
    EIdReader.removeListeners();
  }
}
```

See the [example app](./example) for a fuller, runnable integration (including input UI for the MRZ fields).

## API

### `startReading(params): Promise<EIdReadResult>`

Starts an NFC reading session. On iOS this presents the system NFC sheet; on Android the user must hold the document against the back of the phone.

Exactly one of `mrzInfo` or `can` must be supplied.

| Param            | Type                                            | Default | Description                                                                                                  |
| ---------------- | ----------------------------------------------- | ------- | ------------------------------------------------------------------------------------------------------------ |
| `mrzInfo`        | `{ documentNumber, birthDate, expirationDate }` | —       | MRZ-derived key for BAC / PACE-MRZ. Dates in `YYMMDD` format. `documentNumber` may be longer than 9 chars for TD1 cards with an extended document number. |
| `can`            | `string` (6 digits)                             | —       | Card Access Number for PACE-CAN. iOS only for now.                                                           |
| `includeImages`  | `boolean`                                       | `false` | Include the portrait photo from DG2                                                                          |
| `includeRawData` | `boolean`                                       | `false` | Include raw data groups as base64                                                                            |
| `labels`         | `object`                                        | —       | Strings shown in the iOS NFC sheet & errors                                                                  |

Returns an `EIdReadResult` whose `status` is `'OK' | 'Error' | 'Canceled'`.

### Authentication protocol used

The library picks the strongest access protocol the card advertises:

1. **PACE** (preferred) — negotiated automatically when the card publishes `PACEInfo` in `EF.CardAccess`.
   - `PACE-MRZ` when `mrzInfo` is supplied.
   - `PACE-CAN` when `can` is supplied (iOS only for now).
2. **BAC** — automatic fallback when PACE is not supported or fails, using the MRZ key. Only meaningful with `mrzInfo`; eIDs that refuse BAC (e.g. French CNIe) will simply report the PACE failure.

### `stopReading(): void`

Aborts the current reading session.

### `isNfcSupported(): Promise<boolean>`

Whether the device has NFC hardware.

### `isNfcEnabled(): Promise<boolean>`

Whether NFC is currently turned on (Android).

### `openNfcSettings(): Promise<boolean>`

Opens the system NFC settings screen (Android).

### `imageDataUrlToJpegDataUrl(dataUrl): string`

Converts a JPEG2000 data URL (as produced by the passport chip for the portrait) to a JPEG data URL that standard `<Image />` components can render.

### Events

- `addOnTagDiscoveredListener(cb)` — fires when an NFC tag enters the field
- `addOnNfcStateChangedListener(cb)` — fires when NFC is turned on/off (`'on' | 'off'`)
- `removeListeners()` — removes all listeners added by this module

## Running the example

```sh
yarn
yarn example start         # Metro
yarn example android       # or
yarn example ios           # (after `cd example/ios && pod install`)
```

## Standards & references

- [ICAO Doc 9303 — Machine Readable Travel Documents](https://www.icao.int/publications/pages/publication.aspx?docnum=9303)
- [BSI TR-03110 — Advanced Security Mechanisms for Machine Readable Travel Documents](https://www.bsi.bund.de/) (PACE / EAC)
- PACE (Password Authenticated Connection Establishment) — secure session establishment from an MRZ key or CAN
- BAC (Basic Access Control) — legacy access protocol derived from the MRZ
- DG1 (MRZ) and DG2 (portrait) are the data groups parsed by default

## Acknowledgements & project history

This module stands on the shoulders of two excellent open-source projects:

- **[batuhanoztrk/react-native-nfc-passport-reader](https://github.com/batuhanoztrk/react-native-nfc-passport-reader)** — the starting point for the Android implementation and the public JS API. When we forked, that library only supported Android.
- **[AndyQ/NFCPassportReader](https://github.com/AndyQ/NFCPassportReader)** — a mature Swift implementation of ICAO 9303 passport reading, which we used to add iOS support.

At the time we added iOS support, consuming `NFCPassportReader` as a normal Swift/CocoaPods dependency was impractical in a React Native context for two reasons:

1. **OpenSSL version conflicts** — the library depends on a specific `OpenSSL-Universal` version, and resolving it alongside other transitive pods in a React Native app was fragile.
2. **Swift-only module integration** — React Native's iOS build setup around that era required `use_frameworks!` to consume Swift-only pods, which interacted poorly with Hermes and static React-Core pods.

As a pragmatic workaround, we **vendored** the relevant Swift sources into `ios/NFCPassportReader/` and pinned `OpenSSL-Universal` at the podspec level.

### Roadmap

Our long-term plan is to remove the vendored fork and converge on the upstream projects:

1. Contribute bug fixes we've accumulated back to **AndyQ/NFCPassportReader**.
2. Consume `NFCPassportReader` as a proper Swift dependency (CocoaPods or SPM) now that modern React Native (0.76+) has much better Swift interop.
3. Eventually, contribute the iOS implementation back to **batuhanoztrk/react-native-nfc-passport-reader** so the ecosystem converges on a single, cross-platform library.

Contributions toward any of these goals are very welcome — see [CONTRIBUTING.md](./CONTRIBUTING.md).

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the dev workflow and how to submit pull requests.

## License

MIT © [2060.io](https://2060.io)
