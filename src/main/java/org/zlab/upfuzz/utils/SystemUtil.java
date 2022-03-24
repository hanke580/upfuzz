package org.zlab.upfuzz.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class SystemUtil {

    public static Process exec(String[] cmds, File path) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmds);
        Map<String, String> env = pb.environment();
        pb.directory(path);
        Process p = pb.start();
        return p;
    }

    public static Process exec(String[] cmds, String path) throws IOException {
        return exec(cmds, new File(path));
    }

    public static String getMainClassName() throws ClassNotFoundException {
        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            // System.out.println(thread.getThreadGroup().getName() );
            if (thread.getThreadGroup() != null && thread.getThreadGroup().getName().equals("main")) {
                for (StackTraceElement stackTraceElement : entry.getValue()) {
                    // System.out.println(stackTraceElement.getClassName()+ " " + stackTraceElement.getMethodName() + " " + stackTraceElement.getFileName());
                    if (stackTraceElement.getMethodName().equals("main")) {

                        try {
                            Class<?> c = Class.forName(stackTraceElement.getClassName());
                            Class[] argTypes = new Class[] { String[].class };
                            //This will throw NoSuchMethodException in case of fake main methods
                            c.getDeclaredMethod("main", argTypes);
                            // return stackTraceElement.getClassName();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }
}
