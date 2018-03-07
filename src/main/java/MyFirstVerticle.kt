import data.User
import data.VertxUserInMemoryRepository
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Launcher
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import java.util.*

class MyFirstVerticle : AbstractVerticle() {

    private val userRepository = VertxUserInMemoryRepository(createSomeData())

    override fun start(fut: Future<Void>) {
        val router = Router.router(vertx)
        router.route("$USERS_API_BASE_URL*").handler(BodyHandler.create())
        router.post(USERS_API_BASE_URL).handler({ userRepository.addOne(it) })
        router.get(USERS_API_BASE_URL).handler({ userRepository.getAll(it) })
        router.get(USER_BY_ID_API_URL).handler({ userRepository.getOne(it) })
        router.put(USER_BY_ID_API_URL).handler({ userRepository.updateOne(it) })
        router.delete(USER_BY_ID_API_URL).handler({ userRepository.deleteOne(it) })

        router.route(ROOT_API_URL).handler { routingContext ->
            val response = routingContext.response()
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>")
        }

        router.route("/assets/*").handler(StaticHandler.create("assets"))

        vertx
                .createHttpServer()
                .requestHandler({ router.accept(it) })
                .listen(
                        config().getInteger("http.port", 8081)!!
                ) { result ->
                    if (result.succeeded()) {
                        fut.complete()
                    } else {
                        fut.fail(result.cause())
                    }
                }
    }

    private fun createSomeData(): Map<Int, User> {
        val users = LinkedHashMap<Int, User>()
        val firstUser = User("Ivan Petrov")
        users[firstUser.id] = firstUser
        val secondUser = User("Dimitriy Ivanov")
        users[secondUser.id] = secondUser
        return users
    }

    companion object {

        val USERS_API_BASE_URL = "/api/users"
        val USER_BY_ID_API_URL = "$USERS_API_BASE_URL/:id"
        val ROOT_API_URL = "/"

        @JvmStatic
        fun main(args: Array<String>) {
            Launcher.executeCommand("run", MyFirstVerticle::class.java.name)
        }
    }
}