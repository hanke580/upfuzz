package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.ValidState;

import java.util.HashSet;
import java.util.Set;

public class CassandraValidState implements ValidState {
    public Set<CassandraTable> tables = new HashSet<>();

}
