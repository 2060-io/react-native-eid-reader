package io.twentysixty.rn.eidreader.dto

data class EIdData(
  var birthDate: String? = null,
  var placeOfBirth: String? = null,
  var documentNo: String? = null,
  var expiryDate: String? = null,
  var firstName: String? = null,
  var gender: String? = null,
  var identityNo: String? = null,
  var lastName: String? = null,
  var mrz: String? = null,
  var nationality: String? = null,
  var originalFacePhoto: String? = null,
)
