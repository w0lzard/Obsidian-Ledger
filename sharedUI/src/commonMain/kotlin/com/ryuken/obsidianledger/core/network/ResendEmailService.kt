package com.ryuken.obsidianledger.core.network

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ResendEmailService(
    private val supabaseAnonKey : String,
    private val supabaseUrl     : String
) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient         = true
            })
        }
    }

    private val edgeFunctionUrl: String
        get() = "$supabaseUrl/functions/v1/send-payment-request"

    suspend fun sendPaymentRequest(
        toEmail      : String,
        toName       : String,
        fromUserName : String,
        amount       : Double,
        groupName    : String,
        breakdown    : List<Pair<String, Double>>
    ): Result<Unit> {
        return try {
            Napier.d("ResendEmailService: calling Edge Function for $toEmail")

            val response = client.post(edgeFunctionUrl) {
                header("Authorization", "Bearer $supabaseAnonKey")
                header("apikey", supabaseAnonKey)
                contentType(ContentType.Application.Json)
                setBody(
                    EdgeFunctionRequest(
                        toEmail      = toEmail,
                        toName       = toName,
                        fromUserName = fromUserName,
                        amount       = amount,
                        groupName    = groupName,
                        breakdown    = breakdown.map {
                            BreakdownItem(
                                description = it.first,
                                amount      = it.second
                            )
                        }
                    )
                )
            }

            val responseBody = response.bodyAsText()
            Napier.d("ResendEmailService: status=${response.status} body=$responseBody")

            if (response.status.isSuccess()) {
                Napier.d("ResendEmailService: email sent successfully")
                Result.success(Unit)
            } else {
                Napier.e("ResendEmailService: failed — $responseBody")
                Result.failure(Exception("Failed to send email: ${response.status}"))
            }

        } catch (e: Exception) {
            Napier.e("ResendEmailService: exception — ${e.message}", e)
            Result.failure(e)
        }
    }
}

@Serializable
private data class EdgeFunctionRequest(
    @SerialName("toEmail")      val toEmail      : String,
    @SerialName("toName")       val toName       : String,
    @SerialName("fromUserName") val fromUserName : String,
    @SerialName("amount")       val amount       : Double,
    @SerialName("groupName")    val groupName    : String,
    @SerialName("breakdown")    val breakdown    : List<BreakdownItem>
)

@Serializable
private data class BreakdownItem(
    @SerialName("description") val description : String,
    @SerialName("amount")      val amount      : Double
)
