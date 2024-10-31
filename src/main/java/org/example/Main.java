package org.example;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.runtime.jobgraph.JobVertexResourceRequirements;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws Exception {
        Map<String,String> argmap = new HashMap<String,String>();
        if(args.length%2!=0){
            System.out.println("wrong in argument, please check");
            System.exit(0);
        }
        for(int i=0; i<args.length; i+=2) {
            argmap.put(args[i], args[i+1]);
        }
        if(argmap.get("--generator") != null){
            int generator = Integer.parseInt(argmap.get("--generator"));
            if (generator == 1) {
                DataGenerator.dataGenerator();
            }
        }


        Logger logger = Logger.getGlobal();
        // Parameter parsing for parallelism degree
        ParameterTool parameters = ParameterTool.fromArgs(args);
        int parallelismDegree = parameters.getInt("parallelism", 1);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setMaxParallelism(10);
        if(parallelismDegree>env.getMaxParallelism()){
            System.out.println("Parallelism is to big, limit 10, setting parallelism to 10...");
            parallelismDegree= env.getMaxParallelism();
        }
        env.setParallelism(parallelismDegree);
        File file = new File("household_data.txt");
        if(!file.exists()){
            System.out.println("File does not exist");
            System.exit(0);
        }
        DataStreamSource<String> dataStreamSource = env.readTextFile("household_data.txt");
        DataStream<String> output=FlinkJob.flinkJob(dataStreamSource);
        output.print();
        env.execute("Flink Detection on Household Data");
    }
}
