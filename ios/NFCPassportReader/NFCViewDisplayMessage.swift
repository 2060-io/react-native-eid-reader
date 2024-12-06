//
//  NFCViewDisplayMessage.swift
//  NFCPassportReader
//
//  Created by Andy Qua on 09/02/2021.
//

import Foundation

@available(iOS 13, macOS 10.15, *)
public enum NFCViewDisplayMessage {
    case requestPresentPassport(String? = nil)
    case authenticatingWithPassport(Int, String? = nil)
    case readingDataGroupProgress(DataGroupId, Int, String? = nil)
    case error(NFCPassportReaderError, String? = nil)
    case activeAuthentication(String? = nil)
    case successfulRead(String? = nil)
}

@available(iOS 13, macOS 10.15, *)
extension NFCViewDisplayMessage {
    public var description: String {
        switch self {
            case .requestPresentPassport(let label):
                return label ?? "Hold your iPhone near an NFC enabled passport."
            case .authenticatingWithPassport(let progress, let label):
                let progressString = handleProgress(percentualProgress: progress)
                let message = "\(label ?? "Authenticating with passport...")\n\n\(progressString)"
                return message
            case .readingDataGroupProgress(let dataGroup, let progress, let label):
                let progressString = handleProgress(percentualProgress: progress)
                return "\(label ?? "Reading...") \(dataGroup)n\n\(progressString)"
            case .error(let tagError, let label):
                switch tagError {
                    case NFCPassportReaderError.TagNotValid(let label):
                        return label ?? "Tag not valid."
                    case NFCPassportReaderError.MoreThanOneTagFound(let label):
                        return label ?? "More than 1 tags was found. Please present only 1 tag."
                    case NFCPassportReaderError.ConnectionError(let label):
                        return label ?? "Connection error. Please try again."
                    case NFCPassportReaderError.InvalidMRZKey(let label):
                        return "MRZ Key not valid for this document."
                    case NFCPassportReaderError.ResponseError(let description, let sw1, let sw2):
                        return "\(label ?? "Sorry, there was a problem reading the passport. Please try again.") \(description) - (0x\(sw1), 0x\(sw2)"
                    default:
                        return label ?? "Sorry, there was a problem reading the passport. Please try again"
                }
            case .activeAuthentication(let label):
                return label ?? "Authenticating..."
            case .successfulRead(let label):
                return label ?? "Passport read successfully"
        }
    }
    
    func handleProgress(percentualProgress: Int) -> String {
        let p = (percentualProgress/20)
        let full = String(repeating: "🟢 ", count: p)
        let empty = String(repeating: "⚪️ ", count: 5-p)
        return "\(full)\(empty)"
    }
}
