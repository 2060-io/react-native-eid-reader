package io.twentysixty.rn.eidreader.dto

data class EIdReadResult(
        var status: String,
        var data: EIdData? = null,
        var rawDataBase64: MutableMap<String, String>? = null,
)
