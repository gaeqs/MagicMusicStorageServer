package util

import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty

class LockedField<T>(val lock: Lock, private var value: T) {


    operator fun getValue(ref: Any?, property: KProperty<*>): T {
        lock.withLock {
            return value
        }
    }

    operator fun setValue(ref: Any?, property: KProperty<*>, value: T) {
        lock.withLock {
            this.value = value
        }
    }

}