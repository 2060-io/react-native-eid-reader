import Foundation
import React
import CoreNFC

@objc(EidReader)
class EidReader: RCTEventEmitter {
  var isReading : Bool = false
  private let passportReader = PassportReader()

  private let _resolve: RCTPromiseResolveBlock? = nil

  @objc(isNfcSupported:withRejecter:)
  func isNfcSupported(resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    resolve(NFCNDEFReaderSession.readingAvailable)
  }

  @objc(isNfcEnabled:withRejecter:)
  func isNfcEnabled(resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    // TODO
    resolve(NFCNDEFReaderSession.readingAvailable)
  }

  @objc(openNfcSettings:withRejecter:)
  func openNfcSettings(resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    // TODO
    resolve(isReading)
  }

  @objc(startReading:withResolver:withRejecter:)
  func startReading(params: NSDictionary, resolve:@escaping RCTPromiseResolveBlock,reject:@escaping RCTPromiseRejectBlock) -> Void {
    // TODO
    isReading = true
    
    let mrzKey = params["mrz"] as! String
    let includeImages = params["includeImages"] as? Bool
 
    Task {
      do {
        let customMessageHandler : (NFCViewDisplayMessage)->String? = { (displayMessage) in
          switch displayMessage {
          case .requestPresentPassport:
            return "Hold your iPhone near an NFC enabled passport."
          default:
            // Return nil for all other messages so we use the provided default
            return nil
          }
        }
        let passport = try await passportReader.readPassport( mrzKey: mrzKey, useExtendedMode: false,  customDisplayMessage:customMessageHandler)

        var nfcResult: [String: Any] = [:]

        nfcResult["birthDate"] = passport.dateOfBirth
        nfcResult["placeOfBirth"] = passport.placeOfBirth
        nfcResult["documentNo"] = passport.documentNumber
        nfcResult["firstName"] = passport.firstName
        nfcResult["gender"] = passport.gender
        nfcResult["identityNo"] = passport.personalNumber
        nfcResult["lastName"] = passport.lastName
        nfcResult["mrz"] = passport.passportMRZ
        nfcResult["nationality"] = passport.nationality

        passport.dataGroupsRead.forEach { dataGroup in
          print( "Got \(dataGroup) details")
          nfcResult[dataGroup.key.getName()] = Data(dataGroup.value.data).base64EncodedString()
        }
        if let _ = passport.faceImageInfo {
          print( "Got face Image details")
          nfcResult["originalFacePhoto"] = passport.passportImage!.jpegData(compressionQuality: 1)?.base64EncodedString() ?? ""
        }
        resolve(nfcResult)

      } catch {
        print("ERROR!")
        
      }
    }
    
  }

  @objc(stopReading)
  func stopReading() -> Void {
    // TODO
    isReading = false
  }
}
