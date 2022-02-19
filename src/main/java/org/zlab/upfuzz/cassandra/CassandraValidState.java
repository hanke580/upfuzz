package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.State;

import java.util.HashSet;
import java.util.Set;

public class CassandraValidState implements State {
    public Set<CassandraTable> tables = new HashSet<>();

}
