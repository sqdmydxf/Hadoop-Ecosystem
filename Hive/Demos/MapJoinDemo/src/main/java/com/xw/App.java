package com.xw;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * small table customers.txt
 *      1,xw,25
 *      2,zmf,24
 *      3,tom,34
 *      4,tomas,45
 *      5,xavier,25
 * big table orders.txt
 *      1,thinkpad,1
 *      2,iphone,1
 *      3,mac,2
 *      4,acer,2
 *      5,computer,3
 *      6,paper,4
 */

public class App {
    public static void main(String[] args) {
        if (args.length <= 2) {
            System.out.println("need three parameters");
            System.exit(-1);
        }
        try {
            Job job = Job.getInstance();

            job.setJarByClass(App.class);
            job.setJobName("MR map join");

            // load small table into distributed cache
            job.addCacheFile(new URI(args[0]));

            // set the path of big table
            FileInputFormat.addInputPath(job, new Path(args[1]));
            // set the path of result
            FileOutputFormat.setOutputPath(job, new Path(args[2]));

            job.setMapperClass(JoinMapper.class);

            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            job.setNumReduceTasks(0);

            job.waitForCompletion(true);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }
}
