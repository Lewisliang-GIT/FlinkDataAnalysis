package org.example;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.List;
import java.util.Map;

public class DataStreamJob {

	public static void main(String[] args) throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//		env.setParallelism(Integer.parseInt(args[0]));
		DataStreamSource<String> dataStreamSource = env.readTextFile("household_data.txt");

		DataStream<Tuple3<Integer, Long, Double>> tupleStream = dataStreamSource.map(new MapFunction<String, Tuple3<Integer, Long, Double>>() {
			@Override
			public Tuple3<Integer, Long, Double> map(String line) throws Exception {
				String[] fields = line.split(",");
				return new Tuple3<>(
						Integer.parseInt(fields[0]), // Household ID
						Long.parseLong(fields[1]), // Timestamp
						Double.parseDouble(fields[2]) // Reading
				);
			}
		});

		System.out.println(tupleStream.print());
//		DataStream<Households> dataStream=dataStreamSource.map(new MapFunction<String, Households>() {
//			@Override
//			public Households map(String line) throws Exception {
//				String[] fields = line.split(",");
//				Households household = new Households();
//				household.setId(new Integer(fields[0]));
//				household.setTimeStamp(new Long(fields[1]));
//				household.setReadingNumber(new Double(fields[2]));
//				return household;
//			}
//		});
		DataStream<Tuple3<Integer, Long, Double>> windowedAvg = tupleStream
				.keyBy(0) // Key by household ID for per-household analysis
				.timeWindow(Time.hours(6))
				.aggregate(new AvgAggregator()); // Custom aggregator for average
		System.out.println(windowedAvg.print());
		// Find sequences with at least 3 consecutive growing averages using CEP
		DataStream<Tuple3<Integer, Long, Double>> growingAverages = findConsecutiveGrowingAverages(windowedAvg, 3);

		growingAverages.print();
		env.execute();
	}

	public static DataStream<Tuple3<Integer, Long, Double>> findConsecutiveGrowingAverages(
			DataStream<Tuple3<Integer, Long, Double>> windowedAvgStream, int minConsecutive) {

		Pattern<Tuple3<Integer, Long, Double>, ?> pattern = Pattern.<Tuple3<Integer, Long, Double>>begin("start")
				.timesOrMore(minConsecutive)
				.consecutive()
				.where(new SimpleCondition<Tuple3<Integer, Long, Double>>() {
					@Override
					public boolean filter(Tuple3<Integer, Long, Double> integerLongDoubleTuple3) throws Exception {
						return false;
					}

				}); // Custom filter for growing averages

		return CEP.pattern(windowedAvgStream.keyBy(0), pattern) // Key by household ID
				.select(new PatternSelectFunction<Tuple3<Integer, Long, Double>, Tuple3<Integer, Long, Double>>() {
					@Override
					public Tuple3<Integer, Long, Double> select(
							Map<String, List<Tuple3<Integer, Long, Double>>> pattern) throws Exception {
						return pattern.get("start").get(minConsecutive - 1); // Get the last element
					}
				});
	}
}

class AvgAggregator implements AggregateFunction<Tuple3<Integer, Long, Double>, Tuple3<Long, Double, Long>, Tuple3<Integer, Long, Double>> {

	@Override
	public Tuple3<Long, Double, Long> createAccumulator() {
		return Tuple3.of(0L, 0.0, 0L); // (count, sum, timestamp)
	}

	@Override
	public Tuple3<Long, Double, Long> add(Tuple3<Integer, Long, Double> value, Tuple3<Long, Double, Long> accumulator) {
		return Tuple3.of(
				accumulator.f0 + 1,
				accumulator.f1 + value.f2,
				value.f1 // Update timestamp to the latest value
		);
	}

	@Override
	public Tuple3<Integer, Long, Double> getResult(Tuple3<Long, Double, Long> accumulator) {
		return Tuple3.of(accumulator.f0.intValue(), accumulator.f2, accumulator.f1 / accumulator.f0);
	}

	@Override
	public Tuple3<Long, Double, Long> merge(Tuple3<Long, Double, Long> a, Tuple3<Long, Double, Long> b) {
		return Tuple3.of(
				a.f0 + b.f0,
				a.f1 + b.f1,
				Math.max(a.f2, b.f2) // Use the latest timestamp
		);
	}
}
