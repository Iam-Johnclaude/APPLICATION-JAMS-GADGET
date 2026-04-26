package com.jamsgadget.inventory.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamsgadget.inventory.R
import com.jamsgadget.inventory.databinding.ItemInstallmentBinding
import com.jamsgadget.inventory.model.InstallmentData
import java.text.NumberFormat
import java.util.Locale

class InstallmentListAdapter(private val onItemClick: (InstallmentData) -> Unit) :
    ListAdapter<InstallmentData, InstallmentListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstallmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemInstallmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(installment: InstallmentData) {
            binding.apply {
                tvCustomerName.text = installment.customerName
                tvProductName.text = installment.productName
                tvSource.text = installment.financingSource
                tvMonthlyAmount.text = "${formatCurrency(installment.monthlyAmount)} / mo"
                tvTotalAmount.text = formatCurrency(installment.totalAmount)
                tvPaidAmount.text = "Paid: ${formatCurrency(installment.paidAmount)}"
                tvRemainingBalance.text = "${formatCurrency(installment.remainingBalance)} left"
                tvPaidPercent.text = "${installment.progressPercent}% paid"
                pbPayment.progress = installment.progressPercent
                
                val context = root.context
                val colorRes: Int
                val bgColorRes: Int

                when (installment.status) {
                    InstallmentData.Status.ACTIVE -> {
                        colorRes = R.color.status_active
                        bgColorRes = R.color.status_active_light
                    }
                    InstallmentData.Status.PAID -> {
                        colorRes = R.color.status_paid
                        bgColorRes = R.color.status_paid_light
                    }
                    InstallmentData.Status.OVERDUE -> {
                        colorRes = R.color.status_overdue
                        bgColorRes = R.color.status_overdue_light
                    }
                }

                val color = ContextCompat.getColor(context, colorRes)
                val bgColor = ContextCompat.getColor(context, bgColorRes)
                
                viewAccent.setBackgroundColor(color)
                tvStatus.text = installment.status.name.lowercase().replaceFirstChar { it.uppercase() }
                tvStatus.setTextColor(color)
                cardStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(bgColor))
                tvPaidAmount.setTextColor(color)
                pbPayment.progressTintList = ColorStateList.valueOf(color)
            }
        }

        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
            return format.format(amount)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InstallmentData>() {
        override fun areItemsTheSame(oldItem: InstallmentData, newItem: InstallmentData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InstallmentData, newItem: InstallmentData): Boolean {
            return oldItem == newItem
        }
    }
}
