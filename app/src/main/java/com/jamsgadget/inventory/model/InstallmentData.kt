package com.jamsgadget.inventory.model

import java.util.UUID

data class InstallmentData(
    val id: String = UUID.randomUUID().toString(),
    val installmentId: String = "INST-${System.currentTimeMillis().toString().takeLast(6)}",
    val customerId: String = "CUST-${UUID.randomUUID().toString().take(6).uppercase()}",
    val customerName: String = "",
    val email: String = "",
    val productName: String = "",
    val status: Status = Status.ACTIVE,
    val financingSource: String = "",
    val monthlyAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: Long = System.currentTimeMillis()
) {
    enum class Status {
        ACTIVE, PAID, OVERDUE
    }

    val remainingBalance: Double
        get() = totalAmount - paidAmount

    val progressPercent: Int
        get() = if (totalAmount > 0) ((paidAmount / totalAmount) * 100).toInt() else 0
}
