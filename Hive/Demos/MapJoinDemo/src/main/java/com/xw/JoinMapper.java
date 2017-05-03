package com.xw;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class JoinMapper extends Mapper<LongWritable, Text, Text, Text> {
    Map<String, String> customers = new HashMap<>();

    private void readFromLocalCacheFile(Path[] paths) throws IOException {
        System.out.println(paths[0].toString());
        if(paths != null && paths.length > 0) {
            BufferedReader br = new BufferedReader(new FileReader(new File(paths[0].toString())));
            String line;
            int index;
            while((line = br.readLine()) != null) {
                index = line.indexOf(",");
                customers.put(line.substring(0, index), line.substring(index + 1));
            }
        }
    }

    private void readFromCacheFile(Mapper<LongWritable, Text, Text, Text>.Context context) throws IOException {
        URI[] uris = context.getCacheFiles();
        if(uris != null && uris.length > 0) {
            FileSystem fileSystem = FileSystem.get(context.getConfiguration());
            FSDataInputStream fsDataInputStream = fileSystem.open(new Path(uris[0]));
            BufferedReader br = new BufferedReader(new InputStreamReader(fsDataInputStream));
            String line;
            int index;
            while((line = br.readLine()) != null) {
                index = line.indexOf(",");
                customers.put(line.substring(0, index), line.substring(index + 1));
            }
        }
    }

    @Override
    protected void setup(Mapper<LongWritable, Text, Text, Text>.Context context)
            throws IOException, InterruptedException {
        // read small table into memory from distributed cache
        readFromCacheFile(context);
        // getLocalCacheFiles: 1. get cache file to local file system
        //                     2. read small table into memory from local file system
//        readFromLocalCacheFile(context.getLocalCacheFiles());

    }

    @Override
    protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
            throws IOException, InterruptedException {
        String line = value.toString();
        if(null == line || "".equals(line)) {
            return;
        }
        int startIndex = line.indexOf(",");
        int endIndex = line.lastIndexOf(",");
        String cid = line.substring(endIndex + 1);
        if(customers.containsKey(cid)) {
            context.write(new Text(cid), new Text(customers.get(cid) + line.substring(startIndex, endIndex)));
        }
    }
}