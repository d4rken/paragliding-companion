package eu.darken.pgc.common.lists.differ

import eu.darken.pgc.common.lists.ListItem

interface DifferItem : ListItem {
    val stableId: Long

    val payloadProvider: ((DifferItem, DifferItem) -> DifferItem?)?
        get() = { old, new ->
            if (new::class.isInstance(old)) new else null
        }
}