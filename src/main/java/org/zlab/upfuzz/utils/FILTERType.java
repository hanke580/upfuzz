package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

/*
*  * Filter builder filters:
 * https://github.com/apache/hbase/blob/826fb411c8106108577952f7e33abfc8474a62a5/hbase-server/src/test/java/org/apache/hadoop/hbase/filter/TestParseFilter.java#L63

 *  https://github.com/apache/hbase/blob/826fb411c8106108577952f7e33abfc8474a62a5/src/main/asciidoc/_chapters/thrift_filter_language.adoc#L192
 *
 *  DependentColumnFilter
 *       no clue about args
 * KeyOnlyFilter
 *      no args,
 * ColumnCountGetFilter
 *      arg1 : number, restricts # of unique columns in result?
 * SingleColumnValueFilter
 *      {FILTER => "SingleColumnValueFilter ('columnFamily1', 'C0', <=, 'binary:value')"}
 *      don't understand
 * PrefixFilter
 *      count 'foo', {FILTER => "PrefixFilter('r1')"} // prefix for row
 * SingleColumnValueExcludeFilter
 *
 * FirstKeyOnlyFilter
 *      {FILTER => "FirstKeyOnlyFilter()"}
 *      the first kv pair of each row
 * ColumnRangeFilter
 *      scan 'foo', {FILTER => "ColumnRangeFilter ('c0', true, 'c1', true)"} // 2 booleans to include/exclude lower & upper bounds
 * ColumnValueFilter
 *      filters values of cell in columnFamily1:c1 {FILTER => "ColumnValueFilter ('columnFamily1', 'c1', <, 'binaryprefix:v5')"}
 * TimestampsFilter
 *      skip for now: need to store timestamps for this
 * FamilyFilter
 *      {FILTER => "FamilyFilter(>=, 'binaryprefix:columnFamily1')"} aka (binary operator, comparator)
 *            comparator:
 *                  The general syntax of a comparator is: ComparatorType:ComparatorValue
 *                      The ComparatorType for the various comparators is as follows:
 *                           BinaryComparator - binary
 *                           BinaryPrefixComparator - binaryprefix
 *                           RegexStringComparator - regexstring
 *                           SubStringComparator - substring
 *                      The ComparatorValue can be any value.
 * QualifierFilter
 *      scan 'foo', {FILTER => "QualifierFilter(<=, 'binaryprefix:c01')"}
 * ColumnPrefixFilter
 *       1 arg, the prefix: {FILTER => "ColumnPrefixFilter('01')"}
 * RowFilter
 *      binary comparison op, comparator
 *      scan 'foo', {FILTER => "RowFilter(>=, 'binary:r4')"}
 * MultipleColumnPrefixFilter
 *      skip for now
 * InclusiveStopFilter
 *       returns entries upto (not including argument row) (non-existent row returns all/none/some)
 *      {FILTER => "InclusiveStopFilter( 'r4')"}
 * PageFilter
 *      1 arg, num, max # of rows to return
 *       {FILTER => "PageFilter(1)"}
 * ValueFilter
 *      will have to use random values?
 *      binary comp op, comparator
 *       scan 'foo', {FILTER => "ValueFilter(>=, 'binary:v3')"}
 * ColumnPaginationFilter
 *      ColumnPaginationFilter(x, y) returns, for each row, first x columns after y columns, for all rows
 *          for rows w <= x columns, no entries from that row will be present in result
 *
* */

public class FILTERType extends ParameterType.ConcreteType {

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        return null;
    }

    @Override
    public String generateStringValue(Parameter p) {
        return null;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {

    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        return false;
    }
}