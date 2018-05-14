import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.util.ArrayList;
import java.util.List;

public class InvertedIndexer extends Configured {
    public static void main(String[] args) throws Exception{
        /*
        * set up HBase connection
        * */
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Admin hBaseAdmin = connection.getAdmin();

        TableName tableName = TableName.valueOf("TermFrequency");

        // drop out-of-date table
        if (hBaseAdmin.tableExists(tableName)){
            hBaseAdmin.disableTable(tableName);
            hBaseAdmin.deleteTable(tableName);
        }

        // table definition
        TableDescriptorBuilder tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(tableName);
        List<ColumnFamilyDescriptor> columns = new ArrayList<ColumnFamilyDescriptor>();
        columns.add(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("properties")).build());
        tableDescriptorBuilder.setColumnFamilies(columns);
        hBaseAdmin.createTable(tableDescriptorBuilder.build());

        hBaseAdmin.close();

        /*
        * initialize job
        * */
        // set configurations
        conf.set("HDFSOutputFileName", "Inverted-Index");
        conf.set("HDFSOutputPath", args[1]);
        Job job = Job.getInstance(conf,"inverted index + HBase & HDFS");
		job.setJarByClass(InvertedIndexer.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));

        // mapper from HDFS file
        job.setMapperClass(InvertedIndexerMapper.class);
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(IntWritable.class);

        // intermediate layers
        job.setCombinerClass(SumCombiner.class);

        // reducer for HBase Table
        TableMapReduceUtil.initTableReducerJob(tableName.getNameAsString(), InvertedIndexerReducer.class, job, InvertedIndexerPartitioner.class);
        job.setOutputKeyClass(ImmutableBytesWritable.class);
        job.setOutputValueClass(Put.class);

        // multipleOutput: HDFS file
        MultipleOutputs.addNamedOutput(job, conf.get("HDFSOutputFileName"), TextOutputFormat.class, Text.class, Text.class);

        // majorOutput: HBase table
        job.setOutputFormatClass(TableOutputFormat.class);

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
