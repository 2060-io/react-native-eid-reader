package io.twentysixty.rn.eidreader

import android.content.Context
import android.nfc.tech.IsoDep
import android.util.Base64
import io.twentysixty.rn.eidreader.utils.*
import io.twentysixty.rn.eidreader.dto.*
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardSecurityFile
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.iso19794.FaceImageInfo


class EIdReader(context: Context) {
  private val bitmapUtil = BitmapUtil(context)
  private val dateUtil = DateUtil()

  fun readPassport(isoDep: IsoDep, bacKey: BACKeySpec, includeImages: Boolean, includeRawData: Boolean): EIdReadResult {
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

    var paceSucceeded = false
    try {
      val cardSecurityFile =
        CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY))
      val securityInfoCollection = cardSecurityFile.securityInfos

      for (securityInfo in securityInfoCollection) {
        if (securityInfo is PACEInfo) {
          service.doPACE(
            bacKey,
            securityInfo.objectIdentifier,
            PACEInfo.toParameterSpec(securityInfo.parameterId),
            null
          )
          paceSucceeded = true
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    service.sendSelectApplet(paceSucceeded)

    val nfcResult = EIdData()
    val dataGroupData: MutableMap<String, String> = mutableMapOf()

    if (!paceSucceeded) {
      try {
        service.getInputStream(PassportService.EF_COM).read()
      } catch (e: Exception) {
        e.printStackTrace()

        service.doBAC(bacKey)
      }
    }

    if (includeRawData) {
      val comIn = service.getInputStream(PassportService.EF_COM)
      val comFile = comIn.readBytes()
      dataGroupData["COM"] = Base64.encodeToString(comFile, Base64.NO_WRAP)
    }

    val dg1In = service.getInputStream(PassportService.EF_DG1)
    val dg1File = DG1File(dg1In)
    if (includeRawData) {
      dataGroupData["DG1"] = Base64.encodeToString(dg1File.encoded, Base64.NO_WRAP)
    }

    val mrzInfo = dg1File.mrzInfo

    try {
      val dg11In = service.getInputStream(PassportService.EF_DG11)
      val dg11File = DG11File(dg11In)
      dataGroupData["DG11"] = Base64.encodeToString(dg11File.encoded, Base64.NO_WRAP)

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
      val dg2In = service.getInputStream(PassportService.EF_DG2)
      val dg2File = DG2File(dg2In)
      val faceInfos = dg2File.faceInfos
      val allFaceImageInfos: MutableList<FaceImageInfo> = ArrayList()
      for (faceInfo in faceInfos) {
        allFaceImageInfos.addAll(faceInfo.faceImageInfos)
      }
      if (allFaceImageInfos.isNotEmpty()) {
        val faceImageInfo = allFaceImageInfos.iterator().next()
        val image = bitmapUtil.getImage(faceImageInfo)
        nfcResult.originalFacePhoto = image
      }
      if (includeRawData) {
        dataGroupData["DG2"] = Base64.encodeToString(dg2File.encoded, Base64.NO_WRAP)
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
