package org.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.functions.MapFunction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;
import java.util.logging.Logger;

// Define the Record structure
class Reading {
	public int householdId;
	public LocalDateTime timestamp;
	public double reading;

	public Reading(int householdId, LocalDateTime timestamp, double reading) {
		this.householdId = householdId;
		this.timestamp = timestamp;
		this.reading = reading;
	}
}

// Define Average calculation structure
class HouseholdAverage {
	public int householdId;
	public double average;
	public LocalDateTime timestamp;

	public int getHouseholdId() {
		return householdId;
	}

	public void setHouseholdId(int householdId) {
		this.householdId = householdId;
	}

	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public HouseholdAverage(int householdId, double average, LocalDateTime timestamp) {
		this.householdId = householdId;
		this.average = average;
		this.timestamp = timestamp;
	}
}

public class FlinkJob {
	public static void main(String[] args) throws Exception {
		Logger logger = Logger.getGlobal();
		// Parameter parsing for parallelism degree
		ParameterTool parameters = ParameterTool.fromArgs(args);
		int parallelismDegree = parameters.getInt("parallelism", 1);

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setParallelism(parallelismDegree);
		DataStreamSource<String> dataStreamSource = env.readTextFile("household_data.txt");


		DataStream<Reading> readings = dataStreamSource.map((MapFunction<String, Reading>) line -> {
			String[] fields = line.split(",");
			Reading reading = new Reading(Integer.parseInt(fields[0]), LocalDateTime.parse(fields[1]), Double.parseDouble(fields[2]));
			return reading;
		});

		DataStream<HouseholdAverage> averages = readings
				.assignTimestampsAndWatermarks(
						WatermarkStrategy.<Reading>forMonotonousTimestamps()
								.withTimestampAssigner((element, recordTimestamp) -> element.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
				.keyBy(reading -> reading.householdId)
				.window(TumblingEventTimeWindows.of(Time.hours(6)))
				.apply(new WindowFunction<Reading, HouseholdAverage, Integer, TimeWindow>() {
					@Override
					public void apply(Integer householdId, TimeWindow window, Iterable<Reading> input, Collector<HouseholdAverage> out) {
						double sum = 0;
						int count = 0;
						LocalDateTime endTime = null;
						for (Reading r : input) {
							sum += r.reading;
							count++;
							endTime = r.timestamp;
						}
						out.collect(new HouseholdAverage(householdId, sum / count, endTime));
					}
				});

		Pattern<HouseholdAverage, ?> pattern = Pattern.<HouseholdAverage>begin("first")
				.next("second").where(new IterativeCondition<HouseholdAverage>() {
					@Override
					public boolean filter(HouseholdAverage value, Context<HouseholdAverage> ctx) throws Exception {
						double avga = value.average;
						double avgb = value.average;
						for (HouseholdAverage event : ctx.getEventsForPattern("first")) {
							avgb = event.average;
						}
						return avga > avgb;
					}}).next("third").

				where(new IterativeCondition<HouseholdAverage>() {
					@Override
					public boolean filter(HouseholdAverage value, Context<HouseholdAverage> ctx) throws Exception {
						double avga = value.average;
						double avgb = value.average;
						for (HouseholdAverage event : ctx.getEventsForPattern("second")) {
							avgb = event.average;
						}
						return avga > avgb;
					}});

//        Pattern<HouseholdAverage, ?> pattern = Pattern.<HouseholdAverage>begin("start").where(new IterativeCondition<HouseholdAverage>() {
//            @Override
//            public boolean filter(HouseholdAverage value, Context<HouseholdAverage> ctx) throws Exception {
//                return value.getAverage()>0;
//            }
//        }).times(3);



		PatternStream<HouseholdAverage> patternStream = CEP.pattern(averages.keyBy(avg -> avg.householdId), pattern);

		DataStream<String> output= patternStream
				.process(new PatternProcessFunction<HouseholdAverage, String>() {
					@Override
					public void processMatch(Map<String, List<HouseholdAverage>> map, Context context, Collector<String> collector) throws Exception {
						List<HouseholdAverage> startEvent = map.get("first");
//                        HouseholdAverage middle = map.get("second").get(0);
//                        HouseholdAverage end = map.get("third").get(0);

						collector.collect(startEvent.get(0).toString());

					}
//                    @Override
//                    public void processMatch(Map<String, List<HouseholdAverage>> match, Context context, Collector<String> out) throws Exception {
//                        List<HouseholdAverage> sequence = match.get("first");
//                        out.collect("Detected consecutive increase in average for household: " + sequence.get(0).householdId);
//                    }
				});

		output.print();
		env.execute("Flink CEP Pattern Detection on Household Data");
	}

}
