package org.zlab.upfuzz.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SerializationInfo {

    public static class SerializePoint {

        public enum Type {
            fieldRef, iterator, collectionGet, arrayRef
        }

        public enum PrintableType {
            byte_, int_, long_, float_, double_, char_, boolean_, string_, enum_;

            public static PrintableType fromType(String s) {
                switch (s) {
                case "byte":
                case "java.lang.Byte":
                    return byte_;
                case "int":
                case "java.lang.Integer":
                    return int_;
                case "long":
                case "java.lang.Long":
                    return long_;
                case "float":
                case "java.lang.Float":
                    return float_;
                case "double":
                case "java.lang.Double":
                    return double_;
                case "char":
                    return char_;
                case "boolean":
                case "java.lang.Boolean":
                    return boolean_;
                case "java.lang.String":
                    return string_;
                case "enum":
                    return enum_;
                default:
                    return null;
                }
            }
        }

        // Location
        public String className;
        public String methodName;
        public int lineNumber;

        public boolean isStatic;
        public boolean isPrintableType; // field
        public PrintableType printableType;

        public Type type;

        public String parentName; // could be this, null if static
        public String fieldName; // handle collection in a special way

        public SerializePoint() {
        }

        public SerializePoint(String className, String methodName,
                int lineNumber, boolean isStatic,
                boolean isPrintableType, PrintableType printableType, Type type,
                String parentName,
                String fieldName) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.isStatic = isStatic;
            this.isPrintableType = isPrintableType;
            this.printableType = printableType;
            this.type = type;
            this.parentName = parentName;
            this.fieldName = fieldName;
        }

        // Equal methods, return 2 objects to be equal as long as the fields are
        // equal
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SerializePoint serializePoint = (SerializePoint) obj;

            boolean isPosEqual = (className.equals(serializePoint.className))
                    && (methodName.equals(serializePoint.methodName))
                    && (lineNumber == serializePoint.lineNumber);

            if (!isPosEqual || isStatic != serializePoint.isStatic
                    || isPrintableType != serializePoint.isPrintableType
                    || printableType != serializePoint.printableType
                    || type != serializePoint.type)
                return false;

            if (type != Type.fieldRef) {
                return parentName == null && serializePoint.parentName == null
                        && fieldName == null
                        && serializePoint.fieldName == null;
            } else {
                return parentName.equals(serializePoint.parentName)
                        && fieldName.equals(serializePoint.fieldName);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName, lineNumber, isStatic,
                    isPrintableType,
                    printableType, type, parentName, fieldName);
        }

        // toString method
        @Override
        public String toString() {
            return "SerializePoint{" + "className='" + className + '\''
                    + ", methodName='"
                    + methodName + '\'' + ", lineNumber=" + lineNumber
                    + ", isStatic=" + isStatic
                    + ", isPrintableType=" + isPrintableType
                    + ", printableType=" + printableType
                    + ", type=" + type + ", parentName='" + parentName + '\''
                    + ", fieldName='"
                    + fieldName + '\'' + '}';
        }
    }

    public static class MergePointInfo implements java.io.Serializable {
        private static final long serialVersionUID = 20231215L;

        public String unitString;
        public String unitType;

        public String varName;
        public String objectClassName;

        public int dumpId;

        public MergePointInfo() {
        }

        public MergePointInfo(String unitString, String unitType,
                String varName,
                String objectClassName, int dumpId) {
            this.unitString = unitString;
            this.unitType = unitType;
            this.varName = varName;
            this.objectClassName = objectClassName;
            this.dumpId = dumpId;
        }

        // implements equal/hashcode
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MergePointInfo other = (MergePointInfo) obj;
            if (objectClassName == null) {
                if (other.objectClassName != null) {
                    return false;
                }
            } else if (!objectClassName.equals(other.objectClassName)) {
                return false;
            }
            if (unitString == null) {
                if (other.unitString != null) {
                    return false;
                }
            } else if (!unitString.equals(other.unitString)) {
                return false;
            }
            if (unitType == null) {
                if (other.unitType != null) {
                    return false;
                }
            } else if (!unitType.equals(other.unitType)) {
                return false;
            }
            if (varName == null) {
                if (other.varName != null) {
                    return false;
                }
            } else if (!varName.equals(other.varName)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((objectClassName == null) ? 0
                    : objectClassName.hashCode());
            result = prime * result
                    + ((unitString == null) ? 0 : unitString.hashCode());
            result = prime * result
                    + ((unitType == null) ? 0 : unitType.hashCode());
            result = prime * result
                    + ((varName == null) ? 0 : varName.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "MergePointInfo [unitString=" + unitString + ", unitType="
                    + unitType
                    + ", varName=" + varName + ", objectClassName="
                    + objectClassName + ", dumpId="
                    + dumpId + "]";
        }
    }

    public static class WritePoint {
        // Location
        public String className;
        public String methodName;
        public int lineNumber;

        public String writeMethodName;
        public boolean isPrintableType; // field
        public SerializationInfo.SerializePoint.PrintableType printableType;

        public WritePoint() {
        }

        public WritePoint(String className, String methodName, int lineNumber,
                String writeMethodName, boolean isPrintableType,
                SerializationInfo.SerializePoint.PrintableType printableType) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.isPrintableType = isPrintableType;
            this.printableType = printableType;
            this.writeMethodName = writeMethodName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            WritePoint writePoint = (WritePoint) obj;

            boolean isPosEqual = (className.equals(writePoint.className))
                    && (methodName.equals(writePoint.methodName))
                    && (lineNumber == writePoint.lineNumber);

            if (!isPosEqual
                    || !writeMethodName.equals(writePoint.writeMethodName)
                    || isPrintableType != writePoint.isPrintableType
                    || printableType != writePoint.printableType)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName, lineNumber,
                    writeMethodName, isPrintableType,
                    printableType);
        }

        @Override
        public String toString() {
            return "WritePoint{" + "className='" + className + '\''
                    + ", methodName='" + methodName
                    + '\'' + ", lineNumber=" + lineNumber
                    + ", writeMethodName='" + writeMethodName
                    + '\'' + ", isPrintableType=" + isPrintableType
                    + ", printableType="
                    + printableType + '}';
        }
    }

    public static class DumpPoint implements java.io.Serializable {
        private static final long serialVersionUID = 20231215L;

        public final String className;
        public final Integer lineNumber;
        public final SerializationInfo.MergePointInfo mergePointInfo;

        public DumpPoint(String className, Integer lineNumber,
                String unitString, String unitType,
                String varName, String objectClassName, int dumpId) {
            this.className = className;
            this.lineNumber = lineNumber;
            mergePointInfo = new SerializationInfo.MergePointInfo(unitString,
                    unitType, varName,
                    objectClassName, dumpId);
        }
    }

    public static class BoundaryBrokenInfo {
        public String collectionVar;
        public String className;
        public Integer lineNumber;
        public Map<String, Set<Integer>> relatedBranches;

        public BoundaryBrokenInfo(String collectionVar, String className,
                Integer lineNumber,
                Map<String, Set<Integer>> relatedBranches) {
            this.collectionVar = collectionVar;
            this.className = className;
            this.lineNumber = lineNumber;
            this.relatedBranches = relatedBranches;
        }
    }

    public static class StackTraceInfo {
        static int id = 0;
        public final List<String> stackTrace;
        public final int stackTraceId;

        public StackTraceInfo(List<String> stackTrace) {
            this.stackTrace = stackTrace;
            this.stackTraceId = id++;
        }

        // toString
        public String toString() {
            return "StackTraceInfo(\n" + "    stackTrace = " + this.stackTrace
                    + "\n"
                    + "    stackTraceId = " + this.stackTraceId + "\n" + ")";
        }
    }
}
