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

        val time = op.startTime.stop()

        // we log to the HDR histogram and do the callback for mutations
        // might extend this to select, but I can't see a reason for it now
        when (op) {
            is Operation.Mutation -> {
                context.metrics.mutationHistogram.recordValue(time)
                runner.onSuccess(op, result)
            }

            is Operation.Deletion -> {
                context.metrics.deleteHistogram.recordValue(time)
            }

            is Operation.SelectStatement -> {
                context.metrics.selectHistogram.recordValue(time)
            }
            is Operation.Stop -> {
                throw OperationStopException()
            }
        }

    }
}