import * as React from 'react';

import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
} from 'react-native';
import EIdReader, {
  type EIdReadResult,
} from '@2060.io/react-native-eid-reader';
import { lena } from './data';

export default function App() {
  const [result, setResult] = React.useState<EIdReadResult>();
  const [convertedImage, setConvertedImage] = React.useState<
    string | undefined
  >(undefined);

  React.useEffect(() => {
    EIdReader.addOnTagDiscoveredListener(() => {
      console.log('Tag Discovered');
    });

    EIdReader.addOnNfcStateChangedListener((state) => {
      console.log('NFC State Changed:', state);
    });

    return () => {
      EIdReader.stopReading();
      EIdReader.removeListeners();
    };
  }, []);

  const startReading = () => {
    EIdReader.startReading({
      mrzInfo: {
        expirationDate: '311201',
        birthDate: '991201',
        documentNumber: '123456789',
      },
      includeRawData: true,
      includeImages: true,
    })
      .then((res) => {
        console.log(`status: ${res.status}`);
        console.log(`result: ${JSON.stringify(res)}`);
        setResult(res);
      })
      .catch((e) => {
        console.error(e.message);
      });
  };

  const convert = () => {
    EIdReader.convert(lena)
      .then((data) => {
        setConvertedImage(`${data}`);
      })
      .catch((error) => {
        console.error('error', error);
      });
  };

  const stopReading = () => {
    EIdReader.stopReading();
  };

  const openNfcSettings = async () => {
    try {
      const result = await EIdReader.openNfcSettings();
      console.log(result);
    } catch (e) {
      console.log(e);
    }
  };

  const isNfcSupported = async () => {
    try {
      const result = await EIdReader.isNfcSupported();
      console.log(result);
    } catch (e) {
      console.log(e);
    }
  };

  const isNfcEnabled = async () => {
    try {
      const result = await EIdReader.isNfcEnabled();
      console.log(result);
    } catch (e) {
      console.log(e);
    }
  };

  return (
    <>
      <ScrollView style={styles.container}>
        <View style={styles.box}>
          <View style={styles.buttonContainer}>
            <TouchableOpacity onPress={convert} style={styles.button}>
              <Text style={styles.buttonText}>Convert</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={startReading} style={styles.button}>
              <Text style={styles.buttonText}>Start Reading</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={stopReading} style={styles.button}>
              <Text style={styles.buttonText}>Stop Reading</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.buttonContainer}>
            <TouchableOpacity onPress={isNfcSupported} style={styles.button}>
              <Text style={styles.buttonText}>Is NFC Supported</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={isNfcEnabled} style={styles.button}>
              <Text style={styles.buttonText}>Is NFC Enabled</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={openNfcSettings} style={styles.button}>
              <Text style={styles.buttonText}>Open NFC Settings</Text>
            </TouchableOpacity>
          </View>

          <Text style={styles.text}>{JSON.stringify(result, null, 2)}</Text>
        </View>
      </ScrollView>
      {convertedImage && (
        <Image source={{ uri: convertedImage }} width={100} height={100} />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#252526',
  },
  buttonContainer: {
    flex: 1,
    flexDirection: 'row',
    gap: 16,
  },
  button: {
    flex: 1,
    backgroundColor: '#fff',
    padding: 16,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 4,
  },
  buttonText: {
    color: '#252526',
    textAlign: 'center',
  },
  text: {
    color: '#fff',
  },
  box: {
    flex: 1,
    padding: 16,
    gap: 8,
  },
  overlayBox: {
    position: 'absolute',
    width: '100%',
    height: '100%',
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 100,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  infoBox: {
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    padding: 16,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    minHeight: 200,
  },
  infoText: {
    color: '#252526',
    textAlign: 'center',
    fontSize: 22,
  },
});
