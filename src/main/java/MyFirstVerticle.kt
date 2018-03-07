import data.VertxUserHsqldbRepository
import data.VertxUserRepository
import io.vertx.core.*
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler


class MyFirstVerticle : AbstractVerticle() {

    private var userRepository: VertxUserRepository? = null

    override fun start(fut: Future<Void>) {
        userRepository = VertxUserHsqldbRepository(
                vertx,
                config(),
                Handler { startWebServer(Handler { event -> completeStartup(event!!, fut) }) },
                fut)
    }

    private fun startWebServer(next: Handler<AsyncResult<HttpServer>>) {
        val router = Router.router(vertx)
        router.route("$USERS_API_BASE_URL*").handler(BodyHandler.create())
        router.post(USERS_API_BASE_URL).handler({ userRepository?.addOne(it) })
        router.get(USERS_API_BASE_URL).handler({ userRepository?.getAll(it) })
        router.get(USER_BY_ID_API_URL).handler({ userRepository?.getOne(it) })
        router.put(USER_BY_ID_API_URL).handler({ userRepository?.updateOne(it) })
        router.delete(USER_BY_ID_API_URL).handler({ userRepository?.deleteOne(it) })

        router.route("/assets/*").handler(StaticHandler.create("assets"))

        vertx
                .createHttpServer()
                .requestHandler({
                    router.accept(it)
                })
                .listen(
                        config().getInteger("http.port", 8081),
                        next)
    }

    private fun completeStartup(http: AsyncResult<HttpServer>, fut: Future<Void>) {
        if (http.succeeded()) {
            fut.complete()
        } else {
            fut.fail(http.cause())
        }
    }

    companion object {

        val USERS_API_BASE_URL = "/api/users"
        val USER_BY_ID_API_URL = "$USERS_API_BASE_URL/:id"

        @JvmStatic
        fun main(args: Array<String>) {
            Launcher.executeCommand("run", MyFirstVerticle::class.java.name)
        }
    }
}