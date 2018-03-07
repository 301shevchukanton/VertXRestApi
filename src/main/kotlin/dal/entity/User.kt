package dal.entity

import java.util.concurrent.atomic.AtomicInteger
import io.vertx.core.json.JsonObject



class User {

    val id: Int

    var name: String? = null

    companion object {
        private val COUNTER = AtomicInteger()
    }

    constructor(name: String) {
        this.id = COUNTER.getAndIncrement()
        this.name = name
    }

    constructor() {
        this.id = COUNTER.getAndIncrement()
    }

    constructor(id: Int, name: String?) {
        this.id = id
        this.name = name
    }

    constructor(json: JsonObject) {
        this.name = json.getString("NAME")
        this.id = json.getInteger("ID")
    }

    fun copy(name: String): User = User(this.id, name)
}