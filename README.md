# Changes
## Changes to file reporter :
1. Change format to
    ```csv
    ,,Mutations,,,Reads,,,Deletes,,,Errors,,InFlights,RequestQueueDepth
    Epoch Time (ms), Elapsed Time (ms),Count,Latency (us) (p99),1min (req/s),Count,Latency (us) (p99),1min (req/s),Count,Latency (us) (p99),1min (req/s),Count,1min (errors/s),Count,Count
    ```
2. Replace `Instant.now()` with `System.currentTimeMillis()` to get timestamp.
3. Add option `--reportinterval` in millisecond to control report interval.
4. Report queuing stats from [driver](https://docs.datastax.com/en/drivers/java/3.11/com/datastax/driver/core/Metrics.html). `InFlights` counts requests that have already been sent to Cassandra and are currently being processed. They are in connections. `RequestQueueDepth`c ounts requests waiting for a connection to be available from the pool.
## Others
1. Set datastax driver timeout.
2. Print timestamp of starting main runner to console.
3. Disable logging failed operations.
4. Disable single line console reporter.
5. Disable driver reporting of metrics through JMX.
The remainder of this page is the same as https://github.com/rustyrazorblade/easy-cass-stress.

---------------------------------

# easy-cass-stress: A workload centric stress tool and framework designed for ease of use.

This project is a work in progress.

cassandra-stress is a configuration-based tool for doing benchmarks and testing simple data models for Apache Cassandra. 
Unfortunately, it can be challenging to configure a workload. There are fairly common data models and workloads seen on Apache Cassandra.  
This tool aims to provide a means of executing configurable, pre-defined profiles.

Full docs are here: https://rustyrazorblade.github.io/easy-cass-stress/

# Installation

The easiest way to get started on Linux is to use system packages.
Instructions for installation can be found here: https://rustyrazorblade.github.io/easy-cass-stress/#_installation


# Building

Clone this repo, then build with gradle:

    git clone https://github.com/rustyrazorblade/easy-cass-stress.git
    cd easy-cass-stress
    ./gradlew shadowJar

Use the shell script wrapper to start and get help:

    bin/easy-cass-stress -h

# Examples

Time series workload with a billion operations:

    bin/easy-cass-stress run BasicTimeSeries -i 1B

Key value workload with a million operations across 5k partitions, 50:50 read:write ratio:

    bin/easy-cass-stress run KeyValue -i 1M -p 5k -r .5


Time series workload, using TWCS:

    bin/easy-cass-stress run BasicTimeSeries -i 10M --compaction "{'class':'TimeWindowCompactionStrategy', 'compaction_window_size': 1, 'compaction_window_unit': 'DAYS'}"

Time series workload with a run lasting 1h and 30mins:

    bin/easy-cass-stress run BasicTimeSeries -d "1h30m"

Time series workload with Cassandra Authentication enabled:

    bin/easy-cass-stress run BasicTimeSeries -d '30m' -U '<username>' -P '<password>'
    **Note**: The quotes are mandatory around the username/password
    if they contain special chararacters, which is pretty common for password

# Generating docs

Docs are served out of /docs and can be rebuild using `./gradlew docs`.