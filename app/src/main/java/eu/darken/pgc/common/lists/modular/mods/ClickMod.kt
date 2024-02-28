package eu.darken.pgc.common.lists.modular.mods

import eu.darken.pgc.common.lists.modular.ModularAdapter

class ClickMod<VHT : ModularAdapter.VH> constructor(
    private val listener: (VHT, Int) -> Unit
) : ModularAdapter.Module.Binder<VHT> {

    override fun onBindModularVH(adapter: ModularAdapter<VHT>, vh: VHT, pos: Int, payloads: MutableList<Any>) {
        vh.itemView.setOnClickListener { listener.invoke(vh, pos) }
    }
}