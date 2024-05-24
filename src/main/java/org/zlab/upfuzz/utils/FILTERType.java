package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

import java.util.ArrayList;
import java.util.Random;

import static org.zlab.upfuzz.hbase.HBaseCommand.*;

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

/*
public class FILTERType extends ParameterType.ConcreteType {

    private ArrayList<ArrayList<Parameter>> params;

    public FILTERType() {
        this.params = new ArrayList<>();
    }

    public static final FILTERType instance = new FILTERType();

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return generateRandomParameter(s, c);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        Parameter tableName = chooseTable(s, c, null);
        assert (s instanceof HBaseState);
        init((HBaseState) s, c);
        return new Parameter(this, genString());
    }

    @Override
    public String generateStringValue(Parameter p) {
        return genString();
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        this.params.clear();
        generateRandomParameter(s, c);
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return this.params.isEmpty();
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        // pick a random index
        Parameter numFilters = this.params.get(0).get(1);
        Random random = new Random();
        // we add 1 because params[0] is [table name, number of filters]; the
        // items from [1] to [params.size() - 1] are filters
        int mutateIdx = random.nextInt(Integer.parseInt(numFilters.toString()))
                + 1;
        this.params.set(mutateIdx, generateSingleFilter((HBaseState) s, c));
        return true;
    }

    private void init(HBaseState state, Command c) {
        Parameter tableName = chooseTable(state, c, null);
        ArrayList<Parameter> metadata = new ArrayList<>();
        metadata.add(tableName); // [0] table name
        if (tableHasNoQualifiers(state, c)) {
            System.out.println(
                    "WARNING: table has no qualifiers: cannot generate filters on an empty table");
            return;
        }
        Parameter numFilters = new INTType(1, 3)
                .generateRandomParameter(state, c);
        metadata.add(numFilters);
        this.params.add(metadata);
        for (int i = 0; i < Integer.parseInt(numFilters.toString()); i++) {
            this.params.add(generateSingleFilter(state, c));
        }
    }

    private String genString() {
        if (this.params.size() == 1) {
            return "";
        }
        Parameter tableName = this.params.get(0).get(0);
        Parameter numFilters = this.params.get(0).get(1);
        StringBuilder sb = new StringBuilder();
        Parameter columnFamilyName, qualifier, qualifier1, numColumns,
                rowPrefix;
        Parameter value, binaryOp, numRows, offset, excludeStart, excludeEnd,
                comparatorType;
        ArrayList<Parameter> current = new ArrayList<>();
        sb.append("FILTER => \" ");
        int idx;
        Parameter filterType;
        for (int i = 0; i < Integer.parseInt(numFilters.toString()); i++) {
            if (i > 0) {
                // TODO: incomplete: support unary ops
                if (Math.random() < 0.5f)
                    sb.append("AND ");
                else
                    sb.append("OR ");
            }
            current = this.params.get(i + 1);
            idx = 0;
            filterType = current.get(idx++);
            switch (filterType.toString()) {
            case "DependentColumnFilter":
                columnFamilyName = current.get(idx++);
                qualifier = current.get(idx++);
                sb.append(String.format("DependentColumnFilter('%s', '%s') ",
                        columnFamilyName.toString(), qualifier.toString()));
                break;

            case "KeyOnlyFilter":
                sb.append("KeyOnlyFilter() ");
                break; // no parameters

            case "ColumnCountGetFilter":
                numColumns = current.get(idx++);
                sb.append(String.format("ColumnCountGetFilter(%s) ",
                        numColumns.toString()));
                break;

            case "SingleColumnValueFilter":
                break;

            case "PrefixFilter":
                // this filter is for row names; they all start with 'uuid...'
                rowPrefix = current.get(idx++);
                sb.append(String.format("PrefixFilter('%s') ",
                        rowPrefix.toString()));
                break;
            case "SingleColumnValueExcludeFilter":
                break;

            case "FirstKeyOnlyFilter":
                sb.append("FirstKeyOnlyFilter() ");
                break;

            case "ColumnRangeFilter":
                qualifier = current.get(idx++);
                excludeStart = current.get(idx++);
                qualifier1 = current.get(idx++);
                excludeEnd = current.get(idx++);
                sb.append(
                        String.format("ColumnRangeFilter ('%s', %s, '%s', %s) ",
                                qualifier.toString(), excludeStart.toString(),
                                qualifier1.toString(), excludeEnd.toString()));
                break;

            case "ColumnValueFilter":
                columnFamilyName = current.get(idx++); // [2] column family name
                qualifier = current.get(idx++);

                binaryOp = current.get(idx++);
                comparatorType = current.get(idx++);

                value = current.get(idx++);

                sb.append(String.format(
                        "ColumnValueFilter ('%s', '%s', %s, '%s:%s') ",
                        columnFamilyName.toString(), qualifier.toString(),
                        binaryOp.toString(), comparatorType.toString(),
                        value.toString()));
                break;
            case "TimestampsFilter":
                break;

            case "FamilyFilter":
                binaryOp = current.get(idx++);
                value = current.get(idx++);

                sb.append(String.format(
                        "FamilyFilter(%s, 'binaryprefix:uuid%s') ",
                        binaryOp.toString(), value.toString()));
                break;

            case "QualifierFilter":
                binaryOp = current.get(idx++);
                value = current.get(idx++);

                sb.append(String.format(
                        "QualifierFilter(%s, 'binaryprefix:uuid%s') ",
                        binaryOp.toString(), value.toString()));
                break;

            case "ColumnPrefixFilter":
                value = current.get(idx++);
                sb.append(String.format("ColumnPrefixFilter('%s') ",
                        value.toString()));
                break;

            case "RowFilter":
                binaryOp = current.get(idx++);
                value = current.get(idx++);
                sb.append(String.format("RowFilter(%s, 'binaryprefix:uuid%s') ",
                        binaryOp.toString(), value.toString()));
                break;
            case "MultipleColumnPrefixFilter":
                break;

            case "InclusiveStopFilter":
                value = current.get(idx++);
                sb.append(String.format(
                        "InclusiveStopFilter('binaryprefix:uuid%s') ",
                        value.toString()));
                break;

            case "PageFilter":
                numRows = current.get(idx++);
                sb.append(String.format("PageFilter(%s) ",
                        numRows.toString()));
                break;

            case "ValueFilter":
                binaryOp = current.get(idx++);
                comparatorType = current.get(idx++);
                value = current.get(idx++);
                sb.append(String.format("ValueFilter(%s, '%s:%s') ",
                        binaryOp.toString(), comparatorType.toString(),
                        value.toString()));
                break;

            case "ColumnPaginationFilter":
                numRows = current.get(idx++);
                offset = current.get(idx++);
                sb.append(String.format("ColumnPaginationFilter(%s, %s) ",
                        numRows.toString(), offset.toString()));
                break;

            default:
                throw new IllegalArgumentException(
                        "invalid filter: " + filterType.toString());
            }

        }
        sb.append("\"");
        return sb.toString();
    }

    private ArrayList<Parameter> generateSingleFilter(HBaseState state,
            Command c) {
        ArrayList<Parameter> current = new ArrayList<>();
        Parameter tableName = chooseTable(state, c, null);
        Parameter columnFamilyName, qualifier, qualifier1, numColumns,
                comparatorType;
        Parameter value, binaryOp, numRows, offset, excludeStart, excludeEnd;
        ParameterType.ConcreteType valueType;
        Parameter FILTER_TYPE = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c_) -> Utilities
                        .strings2Parameters(
                                FILTER_TYPES),
                null).generateRandomParameter(state, c);
        current.add(FILTER_TYPE);
        switch (FILTER_TYPE.toString()) {
        case "DependentColumnFilter":
            columnFamilyName = chooseNotEmptyColumnFamily(state, c, null);
            current.add(columnFamilyName); // [2] column family name
            // not sure if the qualifier needs to be present in the selected
            // column family
            qualifier = state.table2families.get(tableName.toString())
                    .get(columnFamilyName.toString()).getRandomQualifier();
            current.add(qualifier);
            break;
        case "KeyOnlyFilter":
            break; // no parameters
        case "ColumnCountGetFilter":
            numColumns = new INTType(1, 10).generateRandomParameter(state, c);
            current.add(numColumns);
            break;
        case "SingleColumnValueFilter":
            // TODO
            break;
        case "PrefixFilter":
            // this filter is for row names; they all start with 'uuid...'
            ParameterType.ConcreteType rowPrefixPart2Type = new ParameterType.NotEmpty(
                    new STRINGType(3));
            Parameter rowPrefixPart2 = rowPrefixPart2Type
                    .generateRandomParameter(state, c);
            current.add(rowPrefixPart2);
            break;
        case "SingleColumnValueExcludeFilter":
            // opposite of "SingleColumnValueFilter", TODO
            break;
        case "FirstKeyOnlyFilter":
            break;
        case "ColumnRangeFilter":
            // we pick two random columns as start and end
            qualifier = chooseNotNullColumn(state, c, null);

            excludeStart = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    COLUMN_RANGE_FILTER_EXCLUDE_START_TYPES),
                    null).generateRandomParameter(state, c);

            qualifier1 = chooseNotNullColumn(state, c, null);

            excludeEnd = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    COLUMN_RANGE_FILTER_EXCLUDE_END_TYPES),
                    null).generateRandomParameter(state, c);

            if (qualifier.toString().compareTo(qualifier1.toString()) > 0) {
                current.add(qualifier);
                current.add(excludeStart);
                current.add(qualifier1);
                current.add(excludeEnd);
            } else {
                current.add(qualifier1);
                current.add(excludeEnd);
                current.add(qualifier);
                current.add(excludeStart);
            }
            break;
        case "ColumnValueFilter":
            columnFamilyName = chooseNotEmptyColumnFamily(state, c, null);
            current.add(columnFamilyName); // [2] column family name
            // not sure if the qualifier needs to be present in the selected
            // column family
            qualifier = state.table2families.get(tableName.toString())
                    .get(columnFamilyName.toString()).getRandomQualifier();
            current.add(qualifier);

            binaryOp = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    BINARY_OPS),
                    null).generateRandomParameter(state, c);
            current.add(binaryOp);

            comparatorType = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    COMPARATOR_TYPES),
                    null).generateRandomParameter(state, c);
            current.add(comparatorType);

            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;
        case "TimestampsFilter":
            break;
        case "FamilyFilter":
            binaryOp = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    BINARY_OPS),
                    null).generateRandomParameter(state, c);
            current.add(binaryOp);

            // since we're filtering column families, we will only use
            // binaryprefix comparator type for now
            // (since names start with 'uuid...'
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;

        case "QualifierFilter":
            binaryOp = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    BINARY_OPS),
                    null).generateRandomParameter(state, c);
            current.add(binaryOp);

            // since we're filtering column names, we will only use binaryprefix
            // comparator type for now
            // (since names start with 'uuid...'
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;

        case "ColumnPrefixFilter":
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;

        case "RowFilter":
            binaryOp = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    BINARY_OPS),
                    null).generateRandomParameter(state, c);
            current.add(binaryOp);

            // since we're filtering column names, we will only use binaryprefix
            // comparator type for now
            // (since row names start with 'uuid...'
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;

        case "MultipleColumnPrefixFilter":
            break;

        case "InclusiveStopFilter":
            // returns entries upto (not including argument row) (non-existent
            // row returns all/none/some)
            // since we're filtering column names, we will only use binaryprefix
            // comparator type for now
            // (since row names start with 'uuid...'
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;

        case "PageFilter":
            numRows = new INTType(1, 10)
                    .generateRandomParameter(state, c);
            current.add(numRows);
            break;

        case "ValueFilter":
            binaryOp = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    BINARY_OPS),
                    null).generateRandomParameter(state, c);
            current.add(binaryOp);

            comparatorType = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    COMPARATOR_TYPES),
                    null).generateRandomParameter(state, c);
            current.add(comparatorType);

            // values to insert are random
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            value = valueType.generateRandomParameter(state, c);
            current.add(value);
            break;

        case "ColumnPaginationFilter":
            numRows = new INTType(1, 10)
                    .generateRandomParameter(state, c);
            current.add(numRows);
            offset = new INTType(1, 10)
                    .generateRandomParameter(state, c);
            current.add(offset);
            break;

        default:
            throw new IllegalArgumentException(
                    "invalid filter: " + FILTER_TYPE);
        }
        return current;
    }
}
 */

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