import * as React from 'react';

import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
  Modal,
  TextInput,
} from 'react-native';
import EIdReader, {
  type EIdReadResult,
} from '@2060.io/react-native-eid-reader';
import { pngExample } from './data';

enum InputStep {
  DocumentNumber = 'DocumentNumber',
  BirthDate = 'BirthDate',
  ExpirationDate = 'ExpirationDate',
}

export default function App() {
  const [result, setResult] = React.useState<EIdReadResult>();
  const [convertedImage, setConvertedImage] = React.useState(pngExample);
  const [modalVisible, setModalVisible] = React.useState(false);
  const [inputStep, setInputStep] = React.useState<InputStep>(
    InputStep.DocumentNumber
  );
  const [documentNumber, setDocumentNumber] = React.useState('33016244');
  const [birthDate, setBirthDate] = React.useState('870624');
  const [expirationDate, setExpirationDate] = React.useState('330501');

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

  const handleOkPress = () => {
    if (inputStep === InputStep.DocumentNumber) {
      console.log('Entered documentNumber:', documentNumber);
      setInputStep(InputStep.BirthDate);
    } else if (inputStep === InputStep.BirthDate) {
      console.log('Entered birthDate:', birthDate);
      setInputStep(InputStep.ExpirationDate);
    } else if (inputStep === InputStep.ExpirationDate) {
      console.log('Entered expirationDate:', expirationDate);
      setInputStep(InputStep.DocumentNumber);
      // Perform action with inputText here
      setModalVisible(false); // Close the modal
      startReading();
    }
  };

  const startReading = () => {
    EIdReader.startReading({
      mrzInfo: {
        documentNumber,
        expirationDate,
        birthDate,
      },
      includeRawData: true,
      includeImages: true,
    })
      .then((res) => {
        console.log(`status: ${res.status}`);
        console.log(`result: ${JSON.stringify(res)}`);
        setResult(res);
        try {
          if (res.data.originalFacePhoto) {
            const img = EIdReader.imageDataUrlToJpegDataUrl(
              `data:image/jp2;base64,${res.data.originalFacePhoto}`
            );
            setConvertedImage(img);
          }
        } catch (error) {
          console.error(error);
        }
      })
      .catch((e) => {
        console.error(e.message);
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

  const texts = {
    [InputStep.DocumentNumber]: 'Document number',
    [InputStep.BirthDate]: 'Birth date (YYMMDD)',
    [InputStep.ExpirationDate]: 'Expiration date (YYMMDD)',
  };

  const callbacks = {
    [InputStep.DocumentNumber]: setDocumentNumber,
    [InputStep.BirthDate]: setBirthDate,
    [InputStep.ExpirationDate]: setExpirationDate,
  };

  const defaultInput = {
    [InputStep.DocumentNumber]: documentNumber,
    [InputStep.BirthDate]: birthDate,
    [InputStep.ExpirationDate]: expirationDate,
  };

  return (
    <>
      <Modal
        animationType="slide"
        transparent={true}
        visible={modalVisible}
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <Text style={styles.modalTitle}>{texts[inputStep]}</Text>
            <TextInput
              style={styles.input}
              value={defaultInput[inputStep]}
              onChangeText={callbacks[inputStep]}
              placeholder="Type something"
            />
            <View style={styles.inputButtonContainer}>
              <TouchableOpacity
                style={styles.inputButton}
                onPress={handleOkPress}
              >
                <Text style={styles.inputButtonText}>OK</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.inputButton}
                onPress={() => setModalVisible(false)}
              >
                <Text style={styles.inputButtonText}>Cancel</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      <ScrollView style={styles.container}>
        <View style={styles.box}>
          <View style={styles.buttonContainer}>
            <TouchableOpacity
              onPress={() => setModalVisible(true)}
              style={styles.button}
            >
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
  modalOverlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  modalContainer: {
    width: 300,
    padding: 20,
    backgroundColor: 'white',
    borderRadius: 10,
    alignItems: 'center',
  },
  modalTitle: {
    fontSize: 18,
    color: 'black',
    fontWeight: 'bold',
    marginBottom: 10,
  },
  inputContainer: {
    flex: 1,
    justifyContent: 'center',
    color: 'black',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  input: {
    width: '100%',
    padding: 10,
    color: 'black',
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 5,
    marginBottom: 20,
  },
  inputButtonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    width: '100%',
  },
  inputButton: {
    backgroundColor: '#007bff',
    padding: 10,
    borderRadius: 5,
  },
  inputButtonText: {
    color: '#fff',
    fontSize: 16,
  },
});
