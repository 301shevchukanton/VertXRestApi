package dal.repository

import io.vertx.ext.web.RoutingContext

interface VertxUserRepository {
    fun addOne(routingContext: RoutingContext)
    fun getAll(routingContext: RoutingContext)
    fun getOne(routingContext: RoutingContext)
    fun updateOne(routingContext: RoutingContext)
    fun deleteOne(routingContext: RoutingContext)
}
