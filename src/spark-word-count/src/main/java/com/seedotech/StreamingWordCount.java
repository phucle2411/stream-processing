package com.seedotech;
import java.util.Arrays;
import java.util.regex.Pattern;

import scala.Tuple2;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

/**
 * Counts words in UTF8 encoded, '\n' delimited text received from the network every second.
 *
 * Usage: StreamingWordCount <hostname> <port>
 * <hostname> and <port> describe the TCP server that Spark Streaming would connect to receive data.
 *
 * To run this on your local machine, you need to first run a Netcat server
 *    `$ nc -lk 9000`
 * and then run the example
 *    `$ bin/run-example com.seedotech.StreamingWordCount localhost 9000`
 */
public final class StreamingWordCount {
    private static final Pattern SPACE = Pattern.compile(" ");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: StreamingWordCount <hostname> <port>");
            System.exit(1);
        }

        // Create the context with a 1 second batch size
//        SparkConf sparkConf = new SparkConf().setAppName("StreamingWordCount");

        // For local debugging/testing
        SparkConf sparkConf = new SparkConf().setAppName("StreamingWordCount").setMaster("local[*]").set("spark.executor.memory","1g");
        JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));

        // Create a JavaReceiverInputDStream on target ip:port and count the
        // words in input stream of \n delimited text (eg. generated by 'nc')
        // Note that no duplication in storage level only for running locally.
        // Replication necessary in distributed scenario for fault tolerance.
        JavaReceiverInputDStream<String> lines = ssc.socketTextStream(args[0], Integer.parseInt(args[1]), StorageLevels.MEMORY_AND_DISK_SER);
        JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
        JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s, 1))
                .reduceByKey((i1, i2) -> i1 + i2);

        wordCounts.print();
        ssc.start();
        ssc.awaitTermination();
    }
}