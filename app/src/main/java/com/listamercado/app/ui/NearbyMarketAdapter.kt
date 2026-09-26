package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.listamercado.app.R
import com.listamercado.app.model.NearbyMarket
import java.util.Locale

class NearbyMarketAdapter(
    private val onFavorite: (NearbyMarket) -> Unit,
    private val onListAction: (NearbyMarket) -> Unit
) : ListAdapter<NearbyMarket, NearbyMarketAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_nearby_market, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textMarketName)
        private val distance: TextView = view.findViewById(R.id.textMarketDistance)
        private val address: TextView = view.findViewById(R.id.textMarketAddress)
        private val source: TextView = view.findViewById(R.id.textMarketSource)
        private val favorite: MaterialButton = view.findViewById(R.id.buttonFavoriteMarket)
        private val listAction: MaterialButton = view.findViewById(R.id.buttonMarketList)

        fun bind(market: NearbyMarket) {
            name.text = if (market.favorite) "★ ${market.name}" else market.name
            distance.text = formatDistance(market.distanceMeters)
            address.text = market.address
            address.visibility = if (market.address.isBlank()) View.GONE else View.VISIBLE
            source.text = if (market.fromCache) {
                "Origem: ${market.source} • cache recente"
            } else {
                "Origem: ${market.source} • Overpass"
            }
            favorite.text = if (market.favorite) "★" else "☆"
            favorite.contentDescription = if (market.favorite) "Remover ${market.name} dos favoritos" else "Favoritar ${market.name}"
            favorite.setOnClickListener { onFavorite(market) }
            listAction.text = if (market.listId != null) "Abrir lista deste mercado" else "Criar lista"
            listAction.setOnClickListener { onListAction(market) }
            itemView.setOnClickListener { onListAction(market) }
        }
    }

    private fun formatDistance(meters: Double): String = if (meters < 1000) {
        "${meters.toInt().coerceAtLeast(0)} m"
    } else {
        String.format(Locale("pt", "BR"), "%.1f km", meters / 1000.0)
    }

    private object Diff : DiffUtil.ItemCallback<NearbyMarket>() {
        override fun areItemsTheSame(oldItem: NearbyMarket, newItem: NearbyMarket): Boolean = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: NearbyMarket, newItem: NearbyMarket): Boolean = oldItem == newItem
    }
}
