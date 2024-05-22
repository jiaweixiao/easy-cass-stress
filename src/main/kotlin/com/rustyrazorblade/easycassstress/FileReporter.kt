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
        TimeUnit.SECONDS,
        TimeUnit.MILLISECONDS
) {

    // UNIX timestamp in ms
    private val startTime = System.currentTimeMillis()

    private val opHeaders = listOf("Count", "Latency (us) (p99)", "1min (req/s)").joinToString(",", postfix = ",")
    private val errorHeaders = listOf("Count", "1min (errors/s)").joinToString(",")

    val outputFile = File(outputFileName)
    val buffer : BufferedWriter

    init {

        buffer = if(outputFileName.endsWith(".gz"))  GZIPOutputStream(outputFile.outputStream()).bufferedWriter() else outputFile.bufferedWriter()

        buffer.write("# easy-cass-stress run at $startTime (unix ts in ms)")
        buffer.newLine()
        buffer.write("# $command")
        buffer.newLine()

        buffer.write(",,Mutations,,,")
        buffer.write("Reads,,,")
        buffer.write("Deletes,,,")
        buffer.write("Errors,")
        buffer.newLine()

        buffer.write("Epoch Time (ms), Elapsed Time (ms),")
        buffer.write(opHeaders)
        buffer.write(opHeaders)
        buffer.write(opHeaders)
        buffer.write(errorHeaders)
        buffer.newLine()
    }

    private fun Timer.getMetricsList(): List<Any> {
        val duration = convertDuration(this.snapshot.get99thPercentile())

        return listOf(this.count, DecimalFormat("##.##").format(duration), DecimalFormat("##.##").format(this.oneMinuteRate))
    }

    override fun report(gauges: SortedMap<String, Gauge<Any>>?,
                        counters: SortedMap<String, Counter>?,
                        histograms: SortedMap<String, Histogram>?,
                        meters: SortedMap<String, Meter>?,
                        timers: SortedMap<String, Timer>?) {

        val timestamp = System.currentTimeMillis()
        val elapsedTime = timestamp - startTime

        buffer.write(timestamp.toString() + "," + elapsedTime.toString() + ",")

        val writeRow = timers!!["mutations"]!!
                .getMetricsList()
                .joinToString(",", postfix = ",")

        buffer.write(writeRow)

        val readRow = timers["selects"]!!
                .getMetricsList()
                .joinToString(",", postfix = ",")

        buffer.write(readRow)

        val deleteRow = timers["deletions"]!!
                .getMetricsList()
                .joinToString(",", postfix = ",")

        buffer.write(deleteRow)

        val errors = meters!!["errors"]!!
        val errorRow = listOf(errors.count, DecimalFormat("##.##").format(errors.oneMinuteRate))
                .joinToString(",", postfix = "\n")

        buffer.write(errorRow)
    }

    override fun stop() {
        buffer.flush()
        buffer.close()
        super.stop()
    }
}
