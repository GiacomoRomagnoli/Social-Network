package social.utils.http

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import java.util.concurrent.CountDownLatch

object TestRequestUtils {
    fun sendPutRequest(
        send: JsonObject?,
        latch: CountDownLatch,
        endpoint: String,
        webClient: WebClient
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.put(endpoint)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(send) { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    fun sendGetRequest(
        paramName: String,
        paramValue: String,
        latch: CountDownLatch,
        endpoint: String,
        webClient: WebClient
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
            .addQueryParam(paramName, paramValue)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .send { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    fun sendGetRequest(
        paramName: String,
        paramValue: String,
        paramName2: String,
        paramValue2: String,
        latch: CountDownLatch,
        endpoint: String,
        webClient: WebClient
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
            .addQueryParam(paramName, paramValue)
            .addQueryParam(paramName2, paramValue2)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .send { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    fun sendGetRequest(
        latch: CountDownLatch,
        endpoint: String,
        webClient: WebClient
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .send { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    fun sendPostRequest(
        send: JsonObject?,
        latch: CountDownLatch,
        endpoint: String,
        webClient: WebClient
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.post(endpoint)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(send) { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    fun post(
        webClient: WebClient,
        endpoint: String,
        body: JsonObject,
        jwt: String? = null,
    ): HttpResponse<String> {
        val latch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.post(endpoint)
            .timeout(5000)
            .putHeader("Content-Type", "application/json")
            .apply { if (jwt != null) putHeader("Authorization", "Bearer $jwt") }
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                if (it.succeeded()) {
                    response = it.result()
                } else {
                    throw it.cause()
                }
                latch.countDown()
            }
        latch.await()
        return response
    }

    fun put(
        webClient: WebClient,
        endpoint: String,
        body: JsonObject,
        jwt: String? = null,
    ): HttpResponse<String> {
        val latch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.put(endpoint)
            .timeout(5000)
            .putHeader("Content-Type", "application/json")
            .apply { if (jwt != null) putHeader("Authorization", "Bearer $jwt") }
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                if (it.succeeded()) {
                    response = it.result()
                } else {
                    throw it.cause()
                }
                latch.countDown()
            }
        latch.await()
        return response
    }

    fun get(
        webClient: WebClient,
        endpoint: String,
        jwt: String? = null,
    ): HttpResponse<String> {
        val latch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
            .timeout(5000)
            .apply { if (jwt != null) putHeader("Authorization", "Bearer $jwt") }
            .`as`(BodyCodec.string())
            .send {
                if (it.succeeded()) {
                    response = it.result()
                } else {
                    throw it.cause()
                }
                latch.countDown()
            }
        latch.await()
        return response
    }
}
