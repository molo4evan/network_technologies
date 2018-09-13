package OS

interface Observable {
    fun addSub(sub: Subscriber)
    fun removeSub(sub: Subscriber)
    fun notifySubs()
}