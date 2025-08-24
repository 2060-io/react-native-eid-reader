package io.twentysixty.rn.eidreader.dto

data class EIdReadResult(
        var status: String,
        var errorMsg: String? = null,
        var data: EIdData? = null,
        var dataGroupsBase64: MutableMap<String, String>? = null,
)
