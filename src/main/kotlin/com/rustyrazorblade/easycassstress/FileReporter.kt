package com.rustyrazorblade.easycassstress

import com.codahale.metrics.*
import com.codahale.metrics.Timer
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream


class FileReporter(registry: MetricRegistry, outputFileName: String, command: String) : ScheduledReporter(registry,
        "file-reporter",
        MetricFilter.ALL,
        TimeUnit.SECONDS, /* rateUnit */
        TimeUnit.MILLISECONDS /* durationUnit */
) {
    private val registry = registry

    // UNIX timestamp in ms
    private val startTime = System.currentTimeMillis()

    private val opHeaders = listOf("Delta Count", "Latency (ms) (p99)", "Latency (ms) (avg)").joinToString(",", postfix = ",")
    private val errorHeaders = listOf("Accum Count", "1min (errors/s)").joinToString(",", postfix = ",")
    private val driverHeaders = "Current Count,Current Count"

    val outputFile = File(outputFileName)
    val buffer : BufferedWriter

    init {

        buffer = if(outputFileName.endsWith(".gz"))  GZIPOutputStream(outputFile.outputStream()).bufferedWriter() else outputFile.bufferedWriter()

        buffer.write("# easy-cass-stress run at $startTime (unix ts in ms)")
        buffer.newLine()
        buffer.write("# $command")
        buffer.newLine()

        buffer.write(",,TmpAllOps,,,")
        buffer.write("Errors,,")
        buffer.write("InFlights,")
        buffer.write("RequestQueueDepth")
        buffer.newLine()

        buffer.write("Epoch Time (ms),Elapsed Time (ms),")
        buffer.write(opHeaders)
        buffer.write(errorHeaders)
        buffer.write(driverHeaders)
        buffer.newLine()
    }

    private fun Histogram.getMetricsList(): List<Any> {
        // histogram is ns
        val durationp99 = this.snapshot.get99thPercentile() / 1_000_000
        val durationavg = this.snapshot.getMean() / 1_000_000

        return listOf(this.count, DecimalFormat("##.##").format(durationp99), DecimalFormat("##.##").format(durationavg))
    }

    override fun report(gauges: SortedMap<String, Gauge<Any>>?,
                        counters: SortedMap<String, Counter>?,
                        histograms: SortedMap<String, Histogram>?,
                        meters: SortedMap<String, Meter>?,
                        timers: SortedMap<String, Timer>?) {

        val timestamp = System.currentTimeMillis()
        val elapsedTime = timestamp - startTime

        buffer.write(timestamp.toString() + "," + elapsedTime.toString() + ",")

        if(histograms != null && histograms.containsKey("tmpallops")) {
            val tmpallopsRow = histograms["tmpallops"]!!
                    .getMetricsList()
                    .joinToString(",", postfix = ",")
            // Remove after report
            registry.remove("tmpallops")
            buffer.write(tmpallopsRow)
        }
        else {
            buffer.write("0,0,0")
        }

        val errors = meters!!["errors"]!!
        val errorRow = listOf(errors.count, DecimalFormat("##.##").format(errors.oneMinuteRate))
                .joinToString(",", postfix = ",")

        buffer.write(errorRow)

        // Metric from driver
        val inflights = gauges!!["in-flight-requests"]!!
                .getValue()
        val rqd = gauges!!["request-queue-depth"]!!
                .getValue()
        val driverRow = "$inflights,$rqd"
        buffer.write(driverRow)
        buffer.newLine()
    }

    override fun stop() {
        buffer.flush()
        buffer.close()
        super.stop()
    }
}
