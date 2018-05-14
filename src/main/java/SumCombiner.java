import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/*
        将key值相同的项合并，减少网络开销
     */
public class SumCombiner extends Reducer<ImmutableBytesWritable,IntWritable,ImmutableBytesWritable,IntWritable> {
    @Override
    public void reduce(ImmutableBytesWritable key, Iterable<IntWritable> values,Context context) throws IOException, InterruptedException{
        int sum = 0;
        for (IntWritable val : values){
            sum += val.get();                                          // 对出现次数求和
        }
        context.write(key,new IntWritable(sum));
    }
}
