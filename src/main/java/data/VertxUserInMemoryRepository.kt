package data

import io.vertx.core.Future
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import java.util.*

class VertxUserInMemoryRepository(users: Map<Int, User>) : VertxUserRepository {

    private val users = LinkedHashMap<Int, User?>()

    companion object {

        val CONTENT_TYPE = "content-type"
        val CONTENT_TYPE_VALUE = "application/json; charset=utf-8"
    }

    init {
        this.users.putAll(users)
    }

    override fun addOne(routingContext: RoutingContext) {
        val user = Json.decodeValue(routingContext.bodyAsString, User::class.java)
        users[user.id] = user
        routingContext.response()
                .setStatusCode(RequestCode.CREATED_STATUS_CODE)
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .end(Json.encodePrettily(user))
    }

    override fun getAll(routingContext: RoutingContext) {
        getDefaultHttpServerResponse(routingContext)
                .end(Json.encodePrettily(users.values))
    }

    override fun getOne(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        if (id == null) {
            routingContext
                    .response()
                    .setStatusCode(RequestCode.BAD_REQUEST_STATUS_CODE)
                    .end()
        } else {
            val idAsInteger = Integer.valueOf(id)
            getDefaultHttpServerResponse(routingContext)
                    .end(Json.encodePrettily(users[idAsInteger]))
        }
    }

    private fun getDefaultHttpServerResponse(routingContext: RoutingContext): HttpServerResponse {
        return routingContext
                .response()
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
    }

    override fun updateOne(routingContext: RoutingContext) {
        val id = routingContext
                .request()
                .getParam("id")
        val json = routingContext.bodyAsJson
        if (id == null || json == null) {
            routingContext
                    .response()
                    .setStatusCode(RequestCode.BAD_REQUEST_STATUS_CODE)
                    .end()
        } else {
            val idAsInteger = Integer.valueOf(id)
            val user = users[idAsInteger]
            if (user == null) {
                routingContext
                        .response()
                        .setStatusCode(RequestCode.NOT_FOUND_STATUS_CODE)
                        .end()
            } else {
                val copyOfUser = users[idAsInteger]?.copy(json.getString("name"))
                users[idAsInteger] = copyOfUser
                getDefaultHttpServerResponse(routingContext)
                        .end(
                                Json.encodePrettily(users[idAsInteger]))
            }
        }
    }

    override fun deleteOne(routingContext: RoutingContext) {
        val id = routingContext
                .request()
                .getParam("id")
        if (id == null) {
            routingContext
                    .response()
                    .setStatusCode(RequestCode.BAD_REQUEST_STATUS_CODE)
                    .end()
        } else {
            val idAsInteger = Integer.valueOf(id)
            users.remove(idAsInteger)
        }
        routingContext
                .response()
                .setStatusCode(RequestCode.NO_CONTENT_STATUS_CODE)
                .end()
    }
}
