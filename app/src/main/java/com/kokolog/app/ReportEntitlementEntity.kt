package com.kokolog.app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 月次詳細レポートの解放履歴。
 *
 * reportMonth : YYYY-MM（対象月）
 * unlockMethod: FREE_TRIAL / PLAY_PURCHASE
 * status      : ACTIVE / REVOKED（無料解放使用済みの事実は行を削除せず残す）
 *
 * Google Play購入情報を端末に保存しても、有効な購入の証明にはならない。
 * 有料権限は Billing 連携時に Play の照会結果をもとに判定すること。
 */
@Entity(tableName = "report_entitlements")
data class ReportEntitlementEntity(
    @PrimaryKey val reportMonth: String,
    val unlockMethod: String,
    val grantedAt: Long,
    val status: String,
    val playProductId: String? = null,
    val purchaseToken: String? = null
) {
    companion object {
        const val FREE_TRIAL = "FREE_TRIAL"
        const val PLAY_PURCHASE = "PLAY_PURCHASE"
        const val ACTIVE = "ACTIVE"
        const val REVOKED = "REVOKED"
    }
}
