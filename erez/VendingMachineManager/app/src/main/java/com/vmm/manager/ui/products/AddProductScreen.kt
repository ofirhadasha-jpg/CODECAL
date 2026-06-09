package com.vmm.manager.ui.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vmm.manager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val form by viewModel.formState.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateForm { copy(imageUri = it) } }
    }

    LaunchedEffect(form.successMessage) {
        if (form.successMessage != null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_product_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.shapes.medium
                    )
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (form.imageUri != null) {
                    AsyncImage(
                        model             = form.imageUri,
                        contentDescription = stringResource(R.string.product_image),
                        modifier          = Modifier.fillMaxSize(),
                        contentScale      = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate, null,
                            modifier = Modifier.size(48.dp),
                            tint     = MaterialTheme.colorScheme.primary
                        )
                        Text(stringResource(R.string.tap_add_image),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.image_specs),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            OutlinedTextField(
                value         = form.name,
                onValueChange = { viewModel.updateForm { copy(name = it) } },
                label         = { Text(stringResource(R.string.field_product_name)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            OutlinedTextField(
                value         = form.barcode,
                onValueChange = { viewModel.updateForm { copy(barcode = it) } },
                label         = { Text(stringResource(R.string.field_barcode)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                trailingIcon  = { Icon(Icons.Default.QrCode, null) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value           = form.costPrice,
                    onValueChange   = { viewModel.updateForm { copy(costPrice = it) } },
                    label           = { Text(stringResource(R.string.field_cost_price)) },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix          = { Text(stringResource(R.string.currency_symbol)) }
                )
                OutlinedTextField(
                    value           = form.salePrice,
                    onValueChange   = { viewModel.updateForm { copy(salePrice = it) } },
                    label           = { Text(stringResource(R.string.field_sale_price)) },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix          = { Text(stringResource(R.string.currency_symbol)) }
                )
            }

            OutlinedButton(
                onClick  = { viewModel.fetchPricingSuggestion() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = form.name.isNotBlank() && !form.isLoading
            ) {
                Icon(Icons.Default.TrendingUp, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_get_price))
            }

            form.pricingSuggestion?.let { s ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.pricing_title), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${stringResource(R.string.min_price)} ₪${"%.2f".format(s.minMarketPrice)}")
                        Text("${stringResource(R.string.max_price)} ₪${"%.2f".format(s.maxMarketPrice)}")
                        Text("${stringResource(R.string.avg_price)} ₪${"%.2f".format(s.avgMarketPrice)}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "${stringResource(R.string.suggested_price)} ₪${"%.2f".format(s.suggestedSalePrice)}",
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${stringResource(R.string.sources)} ${s.sources.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            form.errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick  = { viewModel.saveProduct() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !form.isLoading
            ) {
                if (form.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_save_product), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
