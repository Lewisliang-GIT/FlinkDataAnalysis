package org.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
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

public class FlinkJob {
    public static DataStream<String> flinkJob (DataStream<String> households) {
//        Logger logger = Logger.getGlobal();
//        // Parameter parsing for parallelism degree
//        ParameterTool parameters = ParameterTool.fromArgs(args);
//        int parallelismDegree = parameters.getInt("parallelism", 1);
//
//        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        env.setParallelism(parallelismDegree);
//        DataStreamSource<String> dataStreamSource = env.readTextFile("household_data.txt");


        DataStream<Households> readings = households.map((MapFunction<String, Households>) line -> {
            String[] fields = line.split(",");
            return new Households(Integer.parseInt(fields[0]), LocalDateTime.parse(fields[1]), Double.parseDouble(fields[2]));
        });

        DataStream<HouseholdAverage> averages = readings
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Households>forMonotonousTimestamps()
                                .withTimestampAssigner((element, recordTimestamp) -> element.getTimeStamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .keyBy(Households::getId)
                .window(TumblingEventTimeWindows.of(Time.hours(6)))
                .apply(new WindowFunction<Households, HouseholdAverage, Integer, TimeWindow>() {
                    @Override
                    public void apply(Integer householdId, TimeWindow window, Iterable<Households> input, Collector<HouseholdAverage> out) {
                        double sum = 0;
                        int count = 0;
                        LocalDateTime endTime = null;
                        for (Households r : input) {
                            sum += r.getReadingNumber();
                            count++;
                            endTime = r.getTimeStamp();
                        }
                        out.collect(new HouseholdAverage(householdId, sum / count, endTime));
                    }
                });

        Pattern<HouseholdAverage, ?> pattern = Pattern.<HouseholdAverage>begin("first")
                .next("second").where(new IterativeCondition<HouseholdAverage>() {
                    @Override
                    public boolean filter(HouseholdAverage value, Context<HouseholdAverage> ctx) throws Exception {
                        double avga = value.average;
                        double avgb = 99999;
                        for (HouseholdAverage event : ctx.getEventsForPattern("first")) {
                            avgb = event.average;
                        }
                        return avga > avgb;
                    }
                }).next("third").

                where(new IterativeCondition<HouseholdAverage>() {
                    @Override
                    public boolean filter(HouseholdAverage value, Context<HouseholdAverage> ctx) throws Exception {
                        double avga = value.average;
                        double avgb = 99999;

                        for (HouseholdAverage event : ctx.getEventsForPattern("second")) {
                            avgb = event.average;
                        }
                        return avga > avgb;
                    }
                });

//        Pattern<HouseholdAverage, ?> pattern = Pattern.<HouseholdAverage>begin("start").where(new IterativeCondition<HouseholdAverage>() {
//            @Override
//            public boolean filter(HouseholdAverage value, Context<HouseholdAverage> ctx) throws Exception {
//                return value.getAverage()>0;
//            }
//        }).times(3);
        DataStream<HouseholdAverage> groupById = averages.keyBy(new KeySelector<HouseholdAverage, Object>() {
            @Override
            public Object getKey(HouseholdAverage householdAverage) throws Exception {
                return householdAverage.householdId;
            }
        });


        PatternStream<HouseholdAverage> patternStream = CEP.pattern(groupById, pattern);

        DataStream<String> output = patternStream
                .process(new PatternProcessFunction<HouseholdAverage, String>() {
                    @Override
                    public void processMatch(Map<String, List<HouseholdAverage>> map, Context context, Collector<String> collector) throws Exception {
                        List<HouseholdAverage> startEvent = map.get("first");
                        List<HouseholdAverage> middleEvent = map.get("second");
                        List<HouseholdAverage> lastEvent = map.get("third");

                        collector.collect(startEvent.get(0).toString() + middleEvent.get(0).toString() + lastEvent.get(0).toString());

                    }
//                    @Override
//                    public void processMatch(Map<String, List<HouseholdAverage>> match, Context context, Collector<String> out) throws Exception {
//                        List<HouseholdAverage> sequence = match.get("first");
//                        out.collect("Detected consecutive increase in average for household: " + sequence.get(0).householdId);
//                    }
                });
		return output;

//        output.print();
//        env.execute("Flink CEP Pattern Detection on Household Data");
    }

}
