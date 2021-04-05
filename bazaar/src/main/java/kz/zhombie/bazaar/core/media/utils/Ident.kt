package kz.zhombie.bazaar.core.media.utils

internal data class Ident constructor(
    val id: Long,
    val type: String
)

internal fun getIdentForDocumentId(documentId: String): Ident {
    var ident = Ident(id = -1, type = documentId)
    val split = documentId.indexOf(':')
    ident = if (split == -1) {
        ident.copy(id = -1, type = documentId)
    } else {
        ident.copy(
            id = documentId.substring(split + 1).toLong(),
            type = documentId.substring(0, split)
        )
    }
    return ident
}

internal fun getDocumentIdForIdent(type: String, id: Long): String {
    return "$type:$id"
}