package com.sensacare.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class HealthHistoryItem(
    val date: Date,
    val type: String,
    val value: String,
    val unit: String,
    val status: String
)

class HealthHistoryAdapter(
    private val healthHistory: List<HealthHistoryItem>
) : RecyclerView.Adapter<HealthHistoryAdapter.HealthHistoryViewHolder>() {
    
    class HealthHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvValue: TextView = view.findViewById(R.id.tvValue)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_history, parent, false)
        return HealthHistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HealthHistoryViewHolder, position: Int) {
        val item = healthHistory[position]
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        
        holder.tvDate.text = dateFormat.format(item.date)
        holder.tvType.text = item.type
        holder.tvValue.text = "${item.value} ${item.unit}"
        holder.tvStatus.text = item.status
        
        // Set status color based on the status
        val statusColor = when (item.status.lowercase()) {
            "excellent" -> holder.itemView.context.getColor(R.color.success)
            "good" -> holder.itemView.context.getColor(R.color.info)
            "fair" -> holder.itemView.context.getColor(R.color.warning)
            "poor" -> holder.itemView.context.getColor(R.color.error)
            else -> holder.itemView.context.getColor(R.color.text_secondary)
        }
        
        holder.tvStatus.setTextColor(statusColor)
    }
    
    override fun getItemCount() = healthHistory.size
} 