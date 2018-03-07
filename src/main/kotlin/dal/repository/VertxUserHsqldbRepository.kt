package dal.repository

import dal.entity.User
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.web.RoutingContext
import java.util.stream.Collectors


class VertxUserHsqldbRepository(val vertx: Vertx,
                                val config: JsonObject,
                                startWebServer: Handler<AsyncResult<Void>>,
                                fut: Future<Void>) : VertxUserRepository {
    var jdbc: JDBCClient? = null

    init {
        jdbc = JDBCClient.createShared(vertx, config, "My-Users-Collection");
        startBackend(Handler { connection ->
            createSomeData(connection, startWebServer, fut)
        }, fut)
    }

    private fun startBackend(next: Handler<AsyncResult<SQLConnection>>, fut: Future<Void>) {
        jdbc?.getConnection({ ar ->
            if (ar.failed()) {
                fut.fail(ar.cause())
            } else {
                next.handle(Future.succeededFuture(ar.result()))
            }
        })
    }

    private fun createSomeData(result: AsyncResult<SQLConnection>,
                               next: Handler<AsyncResult<Void>>, fut: Future<Void>) {
        if (result.failed()) {
            fut.fail(result.cause())
        } else {
            val connection = result.result()
            connection.execute(
                    "CREATE TABLE IF NOT EXISTS User (id INTEGER IDENTITY, name varchar(100), " + "origin varchar(100))"
            ) { ar ->
                if (ar.failed()) {
                    fut.fail(ar.cause())
                    connection.close()
                    return@execute
                }
                connection.query("SELECT * FROM User") { select ->
                    if (select.failed()) {
                        fut.fail(ar.cause())
                        connection.close()
                        return@query
                    }
                    if (select.result().numRows == 0) {
                        insert(
                                User("Petrov Dmitriy"),
                                connection, Handler {
                            insert(User("Maxim Olegovich"),
                                    connection, Handler {
                                insert(User("Ivan Ivanovich"),
                                        connection,
                                        Handler {
                                            next.handle(Future.succeededFuture())
                                            connection.close()
                                        })
                            })
                        })
                    } else {
                        next.handle(Future.succeededFuture())
                        connection.close()
                    }
                }
            }
        }
    }

    override fun addOne(routingContext: RoutingContext) {
        jdbc?.getConnection { ar ->
            val user = Json.decodeValue(routingContext.bodyAsString,
                    User::class.java)
            val connection = ar.result()
            insert(user, connection, Handler { r ->
                routingContext.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(r.result()))
            })
            connection.close()
        }
    }

    override fun getAll(routingContext: RoutingContext) {
        jdbc?.getConnection { ar ->
            val connection = ar.result()
            connection.query("SELECT * FROM User") { result ->
                val users = result
                        .result()
                        .rows
                        .stream()
                        .map({ temp ->
                            User(temp)
                        })
                        .collect(Collectors.toList<User>())
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(users))
                connection.close()
            }
        }
    }

    override fun getOne(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        if (id == null) {
            routingContext.response().setStatusCode(400).end()
        } else {
            jdbc?.getConnection { ar ->
                val connection = ar.result()
                select(id, connection, Handler { result ->
                    if (result.succeeded()) {
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(result.result()))
                    } else {
                        routingContext.response()
                                .setStatusCode(404).end()
                    }
                    connection.close()
                })
            }
        }
    }

    override fun updateOne(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        val json = routingContext.bodyAsJson
        if (id == null || json == null) {
            routingContext.response().setStatusCode(400).end()
        } else {
            jdbc?.getConnection { ar ->
                update(id, json, ar.result(), Handler { user ->
                    if (user.failed()) {
                        routingContext.response().setStatusCode(404).end()
                    } else {
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(user.result()))
                    }
                    ar.result().close()
                })
            }
        }
    }

    override fun deleteOne(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        if (id == null) {
            routingContext.response().setStatusCode(400).end()
        } else {
            jdbc?.getConnection { ar ->
                val connection = ar.result()
                connection.execute("DELETE FROM User WHERE id='$id'"
                ) { result ->
                    routingContext.response().setStatusCode(204).end()
                    connection.close()
                }
            }
        }
    }

    private fun insert(user: User, connection: SQLConnection, next: Handler<AsyncResult<User>>) {
        val sql = "INSERT INTO User (name) VALUES ?"
        connection.updateWithParams(sql,
                JsonArray().add(user.name)) { ar ->
            if (ar.failed()) {
                next.handle(Future.failedFuture(ar.cause()))
                return@updateWithParams
            }
            val result = ar.result()
            val w = User(result.keys.getInteger(0), user.name)
            next.handle(Future.succeededFuture(w))
        }
    }

    private fun select(id: String, connection: SQLConnection, resultHandler: Handler<AsyncResult<User>>) {
        connection.queryWithParams("SELECT * FROM User WHERE id=?", JsonArray().add(id)) { ar ->
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture("User not found"))
            } else {
                if (ar.result().numRows >= 1) {
                    resultHandler.handle(Future.succeededFuture(User(ar.result().rows[0])))
                } else {
                    resultHandler.handle(Future.failedFuture("User not found"))
                }
            }
        }
    }

    private fun update(id: String, content: JsonObject, connection: SQLConnection,
                       resultHandler: Handler<AsyncResult<User>>) {
        val sql = "UPDATE User SET name=? WHERE id=?"
        connection.updateWithParams(sql,
                JsonArray().add(content.getString("name")).add(id)
        ) { update ->
            if (update.failed()) {
                resultHandler.handle(Future.failedFuture("Cannot update the user"))
                return@updateWithParams
            }
            if (update.result().updated == 0) {
                resultHandler.handle(Future.failedFuture("User not found"))
                return@updateWithParams
            }
            resultHandler.handle(
                    Future.succeededFuture(User(Integer.valueOf(id),
                            content.getString("name"))))
        }
    }
}
