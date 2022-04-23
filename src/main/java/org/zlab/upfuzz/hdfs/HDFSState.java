package org.zlab.upfuzz.hdfs;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.hdfs.MockFS.INode;
import org.zlab.upfuzz.hdfs.MockFS.LocalFileSystem;

public class HDFSState extends State {
    public HadoopFileSystem dfs = new HadoopFileSystem();
    public LocalFileSystem lfs = new LocalFileSystem();

    public void randomize(double ratio){
        dfs.randomize(ratio);
        lfs.randomize(ratio);
    }

    @Override
    public void clearState() {
        // TODO Auto-generated method stub

    }

    public INode getRandomHadoopPath() {
        return dfs.getRandomPath();
    }

    public String getRandomHadoopPathString() {
        return dfs.getRandomPathString();
    }

    public String getRandomLocalPathString(){
        return lfs.getRandomPathString();
    }
}
