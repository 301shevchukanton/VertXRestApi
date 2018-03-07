package data

import java.util.concurrent.atomic.AtomicInteger

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

    private constructor(id: Int, name: String?) {
        this.id = id
        this.name = name
    }

    fun copy(name: String): User = User(this.id, name)
}