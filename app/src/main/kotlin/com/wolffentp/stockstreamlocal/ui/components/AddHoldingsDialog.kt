package com.wolffentp.stockstreamlocal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for adding or editing a holding (quantity and purchase price).
 * Calls [onConfirm] with (quantity, purchasePrice) when the user submits.
 */
@Composable
fun AddHoldingsDialog(
    symbol: String,
    initialQuantity: Double = 0.0,
    initialPrice: Double = 0.0,
    onConfirm: (quantity: Double, purchasePrice: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var quantityText by remember { mutableStateOf(if (initialQuantity > 0.0) initialQuantity.toString() else "") }
    var priceText by remember { mutableStateOf(if (initialPrice > 0.0) initialPrice.toString() else "") }
    var quantityError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Holding for $symbol") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        quantityError = false
                    },
                    label = { Text("Quantity") },
                    placeholder = { Text("e.g. 100") },
                    isError = quantityError,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = if (quantityError) {
                        @Composable { Text("Please enter a valid quantity") }
                    } else null,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        priceError = false
                    },
                    label = { Text("Purchase Price per Share") },
                    placeholder = { Text("e.g. 150.00") },
                    isError = priceError,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = if (priceError) {
                        @Composable { Text("Please enter a valid price") }
                    } else null,
                )
                Text(
                    "Total Cost Basis: ${calculateCostBasis(quantityText, priceText)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    when {
                        qty <= 0.0 -> quantityError = true
                        price <= 0.0 -> priceError = true
                        else -> {
                            onConfirm(qty, price)
                            onDismiss()
                        }
                    }
                },
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun calculateCostBasis(quantityText: String, priceText: String): String {
    val qty = quantityText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: 0.0
    return if (qty > 0.0 && price > 0.0) {
        val total = qty * price
        String.format("$%.2f", total)
    } else {
        "$0.00"
    }
}
