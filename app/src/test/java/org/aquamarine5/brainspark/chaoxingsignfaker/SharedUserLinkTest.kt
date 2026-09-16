package org.aquamarine5.brainspark.chaoxingsignfaker

import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedUserLinkTest {
    private val query = "phone=13800000000&pwd=a%2Bb%3D&name=%E6%B5%8B%E8%AF%95&face=id1,id1,id2"

    @Test fun importsLocalLinkWithEncodedPasswordAndName() {
        val user = ChaoxingOtherUserSharedEntity.parseFromQRCode("cxsignfaker://import?$query")
        assertEquals("13800000000", user.phoneNumber)
        assertEquals("a+b=", user.encryptedPassword)
        assertEquals("测试", user.userName)
        assertEquals(listOf("id1", "id2"), user.faceObjectIds)
    }

    @Test fun continuesToParseExistingHttpQrCodesLocally() {
        val local = ChaoxingOtherUserSharedEntity.parseFromQRCode("cxsignfaker://import?$query")
        assertEquals(local, ChaoxingOtherUserSharedEntity.parseFromQRCode("https://example.com/?$query"))
    }

    @Test(expected = ChaoxingOtherUserHelper.NotAvailableQRCodeException::class)
    fun rejectsMissingCredentials() {
        ChaoxingOtherUserSharedEntity.parseFromQRCode("cxsignfaker://import?name=test")
    }

    @Test(expected = ChaoxingOtherUserHelper.NotAvailableQRCodeException::class)
    fun rejectsUnrelatedSchemes() {
        ChaoxingOtherUserSharedEntity.parseFromQRCode("custom://import?$query")
    }
}
