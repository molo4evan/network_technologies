class Speeder(private val toMeasure: Downloader): Thread() {
    private val start = System.currentTimeMillis()
    private var fullSize = 0L

    override fun run() {
        var i = 1
        while (!isInterrupted) {
            try{
                Thread.sleep(1000)
            } catch (ex: InterruptedException) {
                showAverageSpeed()
                return
            }
            if (i == 0){
                showMomentSpeed()
                showAverageSpeed()
            }
            fullSize = toMeasure.getRecvSize()
            i = (i + 1) % 3
        }
        showAverageSpeed()
    }

    private fun showMomentSpeed(){
        val received = toMeasure.getRecvSize() - fullSize
        val speed = received.toDouble() / 1024
        println("${toMeasure.header}: Current speed - $speed KB/s")
    }

    private fun showAverageSpeed(){
        fullSize = toMeasure.getRecvSize()
        val curTime = System.currentTimeMillis()
        val fullTime = (curTime.toDouble() - start.toDouble()) / 1000.0
        val avSpeed = (fullSize.toDouble() / fullTime) / 1024.0
        println("${toMeasure.header}: Average speed - $avSpeed KB/s")
    }
}