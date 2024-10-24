import Foundation
import React
import CoreNFC

@objc(EIdReader)
class EIdReader: RCTEventEmitter {
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
    
    let mrzInfo = params["mrzInfo"] as! NSDictionary
    let expirationDate = mrzInfo["expirationDate"] as! String
    let birthDate = mrzInfo["birthDate"] as! String
    let documentNumber = mrzInfo["documentNumber"] as! String
      
    let mrzKey = PassportUtils().getMRZKey(passportNumber: documentNumber, dateOfBirth: birthDate, dateOfExpiry: expirationDate)
    
    let includeImages = params["includeImages"] as? Bool
    let includeRawData = params["includeRawData"] as? Bool

    Task {
      var eidReadResult: [String: Any] = [:]
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

        var data: [String: Any] = [:]

        data["birthDate"] = passport.dateOfBirth
        data["placeOfBirth"] = passport.placeOfBirth
        data["documentNo"] = passport.documentNumber
        data["firstName"] = passport.firstName
        data["gender"] = passport.gender
        data["identityNo"] = passport.personalNumber
        data["lastName"] = passport.lastName
        data["mrz"] = passport.passportMRZ
        data["nationality"] = passport.nationality

        if (includeImages == true) {
            if let _ = passport.faceImageInfo {
              print( "Got face Image details")
              data["originalFacePhoto"] = passport.passportImage!.jpegData(compressionQuality: 1)?.base64EncodedString() ?? ""
           }
        }
          
        eidReadResult["data"] = data

        if (includeRawData == true) {
            var dataGroupsBase64: [String: Any] = [:]
            passport.dataGroupsRead.forEach { dataGroup in
                print( "Got \(dataGroup) details")
                dataGroupsBase64[dataGroup.key.getName()] = Data(dataGroup.value.data).base64EncodedString()
            }
            eidReadResult["dataGroupsBase64"] = dataGroupsBase64
        }
        eidReadResult["status"] = "OK" 
      } catch {
        eidReadResult["status"] = "Error" 
      }
        resolve(eidReadResult)
    }
    
  }

 @objc(convertJp2000ToJpeg:withResolver:withRejecter:)
  func convertJp2000ToJpeg(
    _data: NSString,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let dataSplit = (_data as String).components(separatedBy: ";base64,")
    let mimeType = dataSplit.first?.replacingOccurrences(of: "data:", with: "")
    if(mimeType != "image/jp2"){
        resolve(_data)
        return
    }
    let dataContent = dataSplit[1]
    if let newData = Data(base64Encoded: dataContent) {
        if let jpegData = UIImage(data: newData)?.jpegData(compressionQuality: 1.0)?.base64EncodedString(){
          resolve("data:image/jpeg;base64,\(jpegData)")
          return
        }
    }
    reject("@ConvertError", "Convert from JP2 to JPEG error", nil)
  }

  @objc(stopReading)
  func stopReading() -> Void {
    // TODO
    isReading = false
  }
}
