package com.example.scms

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun ComplaintCard(
    complaint: Complaint,
    onUpvote: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            /* ---------- PHOTO ---------- */
            val imageUrl = complaint.photo_url
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { RetrofitClient.BASE_URL + it }

            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = "Complaint Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            /* ---------- CATEGORY ---------- */
            Text(
                text = complaint.category ?: "Unknown Issue",
                style = MaterialTheme.typography.titleMedium
            )

            /* ---------- REPORTER CREDIT ---------- */
            if (!complaint.reporter_name.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Reported by ${complaint.reporter_name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2563EB)
                    )
                }
                Spacer(Modifier.height(2.dp))
            }

            /* ---------- ADDRESS ---------- */
            Text(
                text = complaint.address ?: "Address not available",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(6.dp))

            /* ---------- LATITUDE & LONGITUDE (SAFE) ---------- */
            val lat = complaint.latitude?.toDoubleOrNull()
            val lon = complaint.longitude?.toDoubleOrNull()

            if (lat != null && lon != null) {
                Text(
                    text = "Location: Lat ${String.format("%.5f", lat)}, " +
                            "Lon ${String.format("%.5f", lon)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            /* ---------- DATE & TIME ---------- */
            Text(
                text = "Reported: ${formatTo12Hour(complaint.created_at)}",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            /* ---------- DESCRIPTION ---------- */
            if (!complaint.description.isNullOrBlank()) {
                Text(complaint.description!!)
            }

            Spacer(modifier = Modifier.height(12.dp))

            /* ---------- STATUS + MAP + UPVOTE ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            complaint.status ?: "Pending",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor =
                            if (complaint.status == "Resolved")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                    )
                )

                Row {

                    /* ---------- MAP ---------- */
                    TextButton(
                        onClick = {
                            if (lat != null && lon != null) {
                                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, uri)
                                )
                            }
                        }
                    ) {
                        Text("Map")
                    }

                    /* ---------- UPVOTE ---------- */
                    Button(onClick = onUpvote) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(complaint.upvotes.toString())
                    }
                }
            }
        }
    }
}
