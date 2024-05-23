package com.rustyrazorblade.easycassstress

import com.datastax.driver.core.ResultSet
import com.google.common.util.concurrent.FutureCallback
import  com.rustyrazorblade.easycassstress.profiles.IStressRunner
import  com.rustyrazorblade.easycassstress.profiles.Operation
import org.apache.logging.log4j.kotlin.logger

/**
 * Callback after a mutation or select
 * This was moved out of the inline ProfileRunner to make populate mode easier
 * as well as reduce clutter
 */
class OperationCallback(val context: StressContext,
                        val runner: IStressRunner,
                        val op: Operation,
                        val populatePhase: Boolean,
                        val paginate: Boolean = false) : FutureCallback<ResultSet> {

    companion object {
        val log = logger()
    }

    override fun onFailure(t: Throwable) {
        context.metrics.errors.mark()
        // log.error { t }

    }

    override fun onSuccess(result: ResultSet) {
        // maybe paginate
        if (paginate) {
            var tmp = result
            while (!tmp.isFullyFetched) {
                tmp = result.fetchMoreResults().get()
            }
        }

        // Return the elapsed time in nanoseconds.
        // https://javadoc.io/static/io.dropwizard.metrics/metrics-core/3.1.2/com/codahale/metrics/Timer.Context.html
        val time = op.startTime.stop()
        var mycount: Long
        var optype: String
        // we log to the HDR histogram and do the callback for mutations
        // might extend this to select, but I can't see a reason for it now
        when (op) {
            is Operation.Mutation -> {
                context.metrics.mutationHistogram.recordValue(time)
                runner.onSuccess(op, result)
                mycount = context.metrics.mutations.count
                optype = "mutate"
            }

            is Operation.Deletion -> {
                context.metrics.deleteHistogram.recordValue(time)
                mycount = context.metrics.deletions.count
                optype = "delete"
            }

            is Operation.SelectStatement -> {
                context.metrics.selectHistogram.recordValue(time)
                mycount = context.metrics.selects.count
                optype = "select"
            }
            is Operation.Stop -> {
                throw OperationStopException()
            }
        }

        if (populatePhase == false && mycount % 10 == 0L) {
            log.info { "$optype $time" }
        }
    }
}