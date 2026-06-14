package de.bierbaum.tradinghelper

import kotlinx.serialization.Serializable

@Serializable
data class TradesResponse(
    val data: List<TradeItem>,
    val meta: MetaInfo
)

@Serializable
data class TradeItem(
    val _tradeId: String? = null,
    val txDate: String,
    val filingDate: String? = null,
    val pubDate: String? = null,
    val txType: String,
    val valueLow: Long? = null,
    val valueHigh: Long? = null,
    val politician: Politician,
    val asset: AssetInfo? = null,
    val issuer: IssuerInfo? = null,
    val size: Int? = null // Some versions of the API might have a size index
) {
    val amountRange: String
        get() = if (valueLow != null && valueHigh != null) {
            "$$valueLow - $$valueHigh"
        } else "Unknown"

    val ticker: String
        get() = asset?.assetTicker?.split(":")?.firstOrNull() ?: "UNKNOWN"
}

@Serializable
data class Politician(
    val _politicianId: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val party: String? = null,
    val chamber: String? = null,
    val stateId: String? = null
)

@Serializable
data class AssetInfo(
    val assetTicker: String? = null,
    val assetName: String? = null,
    val assetType: String? = null
)

@Serializable
data class IssuerInfo(
    val issuerName: String? = null,
    val sector: String? = null
)

@Serializable
data class MetaInfo(
    val paging: PagingInfo
)

@Serializable
data class PagingInfo(
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalItems: Int
)

data class ScoredTrade(
    val trade: TradeItem,
    val score: Int,
    val isCluster: Boolean = false,
    val clusterCount: Int = 0
)
