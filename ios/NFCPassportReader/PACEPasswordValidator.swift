//
//  PACEPasswordValidator.swift
//  NFCPassportReader
//
//  Ported from AndyQ/NFCPassportReader PR #249 by Manwel Bugeja.
//

import Foundation

class PACEPasswordValidator {
    static func validate(password: String, type: PACEPasswordType) throws {
        switch type {
            case .mrz:
                // MRZ validation logic if needed
                break
            case .can:
                guard password.count == 6 && password.allSatisfy({ $0.isNumber }) else {
                    throw NFCPassportReaderError.InvalidDataPassed("CAN must be 6 digits")
                }
        }
    }
}
