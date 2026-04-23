package io.twentysixty.rn.eidreader

import android.content.Context
import android.nfc.tech.IsoDep
import android.util.Base64
import io.twentysixty.rn.eidreader.utils.*
import io.twentysixty.rn.eidreader.dto.*
import java.io.ByteArrayInputStream
import net.sf.scuba.smartcards.CardService
import org.jmrtd.AccessKeySpec
import org.jmrtd.BACKeySpec
import org.jmrtd.PACEKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.iso19794.FaceImageInfo


class EIdReader(context: Context) {
  private val bitmapUtil = BitmapUtil(context)
  private val dateUtil = DateUtil()

  /**
   * Reads the document using the MRZ-derived key. The library will first try
   * PACE-MRZ (if the card advertises PACEInfo in EF.CardAccess) and transparently
   * fall back to BAC on PACE failure or on BAC-only passports.
   */
  fun readPassport(isoDep: IsoDep, bacKey: BACKeySpec, includeImages: Boolean, includeRawData: Boolean): EIdReadResult {
    return readPassportInternal(isoDep, bacKey, bacKey, includeImages, includeRawData)
  }

  /**
   * Reads the document using the Card Access Number (PACE-CAN). BAC fallback
   * is not possible from a CAN, so if PACE fails the session is aborted.
   *
   * NOTE: untested in-house — we currently have no eID that exposes CAN-only
   * access. Reported to work in principle with any jmrtd-compatible card
   * that publishes a PACE OID in EF.CardAccess.
   */
  fun readPassportWithCAN(isoDep: IsoDep, can: String, includeImages: Boolean, includeRawData: Boolean): EIdReadResult {
    val paceKey = PACEKeySpec.createCANKey(can)
    return readPassportInternal(isoDep, paceKey, null, includeImages, includeRawData)
  }

  private fun readPassportInternal(
    isoDep: IsoDep,
    accessKey: AccessKeySpec,
    bacFallbackKey: BACKeySpec?,
    includeImages: Boolean,
    includeRawData: Boolean,
  ): EIdReadResult {
    isoDep.timeout = 5000

    val cardService = CardService.getInstance(isoDep)
    cardService.open()

    val service = PassportService(
      cardService,
      PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
      PassportService.DEFAULT_MAX_BLOCKSIZE,
      false,
      false
    )
    service.open()

    // ICAO 9303-11 §9.2:
    //   EF.CardAccess  (FID 011C) — readable WITHOUT auth. Contains PACEInfo,
    //                                used to bootstrap PACE.
    //   EF.CardSecurity (FID 011D) — readable ONLY AFTER PACE. Used by
    //                                Chip / Terminal Authentication.
    // Earlier revisions of this code read EF.CardSecurity here and got 6A82
    // on modern eIDs (French CNIe etc.) that enforce the distinction, which
    // silently skipped PACE and then failed at SELECT AID with 6982.
    var paceSucceeded = false
    try {
      val cardAccessFile =
        CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
      val securityInfoCollection = cardAccessFile.securityInfos

      for (securityInfo in securityInfoCollection) {
        if (securityInfo is PACEInfo) {
          // jmrtd dispatches on the AccessKeySpec subtype:
          //   BACKey       → PACE-MRZ
          //   PACEKeySpec  → PACE-CAN / PIN / PUK (based on keyReference)
          service.doPACE(
            accessKey,
            securityInfo.objectIdentifier,
            PACEInfo.toParameterSpec(securityInfo.parameterId),
            null
          )
          paceSucceeded = true
          break
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    service.sendSelectApplet(paceSucceeded)

    val nfcResult = EIdData()
    val dataGroupData: MutableMap<String, String> = mutableMapOf()

    if (!paceSucceeded) {
      // Card either has no EF.CardAccess (BAC-only passport) or PACE failed.
      // BAC fallback only makes sense when we were given an MRZ-derived key;
      // you cannot derive BAC keys from a CAN.
      if (bacFallbackKey == null) {
        throw IllegalStateException("PACE failed and no BAC fallback is possible with a CAN-based access key")
      }
      try {
        service.doBAC(bacFallbackKey)
      } catch (e: Exception) {
        e.printStackTrace()
        throw e
      }
    }

    if (includeRawData) {
      val comIn = service.getInputStream(PassportService.EF_COM)
      val comFile = comIn.readBytes()
      dataGroupData["COM"] = Base64.encodeToString(comFile, Base64.NO_WRAP)
    }

    val dg1RawBytes = service.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
    val dg1InputStream = ByteArrayInputStream(dg1RawBytes)
    val dg1File = DG1File(dg1InputStream)

    if (includeRawData) {
      dataGroupData["DG1"] = Base64.encodeToString(dg1RawBytes, Base64.NO_WRAP)
    }

    val mrzInfo = dg1File.mrzInfo

    try {
      val dg11RawBytes = service.getInputStream(PassportService.EF_DG11).use { it.readBytes() }
      val dg11InputStream = ByteArrayInputStream(dg11RawBytes)
      val dg11File = DG11File(dg11InputStream)

      if (includeRawData) {
          dataGroupData["DG11"] = Base64.encodeToString(dg11File.encoded, Base64.NO_WRAP)
      }

      nfcResult.firstName = dg11File.nameOfHolder.substringAfterLast("<<").replace("<", " ")
      nfcResult.lastName = dg11File.nameOfHolder.substringBeforeLast("<<")
  
      nfcResult.placeOfBirth = dg11File.placeOfBirth.joinToString(separator = " ")
      nfcResult.birthDate = dateUtil.convertFromNfcDate(dg11File.fullDateOfBirth)

    } catch (e: Exception) {
      nfcResult.firstName = mrzInfo.secondaryIdentifier.replace("<", " ").trim()
      nfcResult.lastName = mrzInfo.primaryIdentifier.replace("<", " ").trim()
      nfcResult.birthDate = dateUtil.convertFromMrzDate(mrzInfo.dateOfBirth)
    }

    nfcResult.identityNo = mrzInfo.personalNumber
    nfcResult.gender = mrzInfo.gender.toString()

    nfcResult.expiryDate = dateUtil.convertFromMrzDate(mrzInfo.dateOfExpiry)

    nfcResult.documentNo = mrzInfo.documentNumber
    nfcResult.nationality = mrzInfo.nationality
    nfcResult.mrz =
      "${mrzInfo.documentNumber}${mrzInfo.dateOfExpiry}${mrzInfo.dateOfBirth}"

    if (includeImages) {
      val dg2RawBytes = service.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
      val dg2InputStream = ByteArrayInputStream(dg2RawBytes)
      val dg2File = DG2File(dg2InputStream)

      val faceInfos = dg2File.faceInfos
      val allFaceImageInfos: MutableList<FaceImageInfo> = ArrayList()
      for (faceInfo in faceInfos) {
        allFaceImageInfos.addAll(faceInfo.faceImageInfos)
      }
      if (allFaceImageInfos.isNotEmpty()) {
        val faceImageInfo = allFaceImageInfos.iterator().next()
        val image = bitmapUtil.getImage(faceImageInfo.imageInputStream, faceImageInfo.imageLength,faceImageInfo.mimeType)
        nfcResult.originalFacePhoto = image
      }
      if (includeRawData) {
        dataGroupData["DG2"] = Base64.encodeToString(dg2RawBytes, Base64.NO_WRAP)
      }
    }

    if (includeRawData) {
      val sodIn = service.getInputStream(PassportService.EF_SOD)
      val sodFile = sodIn.readBytes()
      dataGroupData["SOD"] = Base64.encodeToString(sodFile, Base64.NO_WRAP)
    }
    return EIdReadResult("OK", nfcResult, dataGroupData)
  }
}
