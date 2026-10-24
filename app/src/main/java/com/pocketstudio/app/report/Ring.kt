package com.pocketstudio.app.report

/**
 * A bounded, ordered memory of the last [capacity] items: adding past it forgets the oldest one.
 * Thread-safe (a lock, so never from the audio thread); nothing is written to disk.
 */
class Ring<T>(val capacity: Int) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val items = arrayOfNulls<Any?>(capacity)
    private var start = 0
    private var count = 0
    private var added = 0L

    /** How many items are kept now (at most [capacity]). */
    val size: Int get() = synchronized(this) { count }

    /** How many items were ever added (the forgotten ones included). */
    val total: Long get() = synchronized(this) { added }

    fun add(item: T) = synchronized(this) {
        items[(start + count) % capacity] = item
        if (count < capacity) count++ else start = (start + 1) % capacity
        added++
    }

    /** The items kept, oldest first. */
    fun snapshot(): List<T> = last(capacity)

    /** The newest [n] items, oldest first. */
    @Suppress("UNCHECKED_CAST")
    fun last(n: Int): List<T> = synchronized(this) {
        val k = n.coerceIn(0, count)
        List(k) { i -> items[(start + count - k + i) % capacity] as T }
    }

    /** The newest item, or null when empty. */
    @Suppress("UNCHECKED_CAST")
    fun lastOrNull(): T? = synchronized(this) { if (count == 0) null else items[(start + count - 1) % capacity] as T }

    fun clear() = synchronized(this) {
        items.fill(null)
        start = 0
        count = 0
    }
}
