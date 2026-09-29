/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.Immutable
import java.net.URI
import java.net.URLDecoder
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper

@Immutable
data class ChaoxingOtherUserSharedEntity(
    val phoneNumber: String,
    val encryptedPassword: String,
    val userName: String,
    val faceObjectIds: List<String> = emptyList(),
    val deviceCode: String? = null,
) {
    companion object {
        fun parseFromQRCode(qrcode: String): ChaoxingOtherUserSharedEntity {
            return runCatching {
                val url = URI(qrcode)
                require(url.scheme in listOf("http", "https", "cxsignfaker"))
                require(url.scheme != "cxsignfaker" || url.host == "import")
                val parameters = requireNotNull(url.rawQuery).split('&').associate { part ->
                    val pair = part.split('=', limit = 2)
                    URLDecoder.decode(pair[0], "UTF-8") to
                        URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
                }
                val phoneNumber = requireNotNull(parameters["phone"])
                val password = requireNotNull(parameters["pwd"])
                val userName = requireNotNull(parameters["name"])
                require(phoneNumber.isNotBlank() && password.isNotBlank() && userName.isNotBlank())
                val faceObjectIds = parameters["face"]
                    ?.split(',')
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    .orEmpty()
                val deviceCode = parameters["dc"]?.takeIf { it.isNotEmpty() }
                ChaoxingOtherUserSharedEntity(
                    phoneNumber,
                    password,
                    userName,
                    faceObjectIds,
                    deviceCode,
                )
            }.getOrElse {
                throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("此二维码不能作用于添加用户")
            }
        }
    }
}
