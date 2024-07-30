package io.github.semutkecil.simplecriteria

import java.io.Serializable

class CacheKey(
    val className: String,
    val select: List<String>,
    val joinKey: List<String>,
    val filterString: String,
    val orderList: MutableList<QueryOrder>,
    var limit: Int? = null,
    var offset: Int? = null,
) : Serializable {
    override fun toString(): String {
        return "$className-${select.sorted().joinToString("||")}-${joinKey.sorted().joinToString("||")}-$filterString-${
            orderList.map { "${it.fieldName}:${it.dir}" }.sorted().joinToString(
                "||"
            )
        }-$limit-$offset"
    }
}