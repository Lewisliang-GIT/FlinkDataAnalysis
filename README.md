# FlinkDataAnalysis

Setting parallelism degree by using ```--parallelism <number>```. Default by 1. 

Setting data generator by using ```--generator <1 generat new data, 0 doesn't generate new data>```

## ```Main```

Running project in Main class. Calling ```DataGenerator.dataGenerator``` to generate date and calling ```FlinkJob.flinkJob``` to process the stream.

## ```DataGenerator```

Generate the data of 10 house by hours randomly. Store the data in file ```household_data.txt```. 

## ```FlinkJob```

Main logic of Flink CEP to query the data. 

## ```Housholds``` and ```HousholdAverarge```

```Housholds``` is class of house data.

```HousholdAverarge``` is a class to store the data process by tumbling windows in 6 hours. Flink CEP will query this type of data stream.

Using Flink to query the data stream is the most challenge part for me. It is a new framework and I never use it before. I need to spend some time familiarising myself with the relevant frameworks. 
Also understanding ```PatternStream``` of Flink CEP and tumbling windows make me confuse at beginning. I need to find out which pattern is fit our goal. Finally, I choose to using strict contiguity pattern to handle the consecutive growing.