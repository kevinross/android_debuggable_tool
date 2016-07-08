package name.kevinross.tool.debuggable;


import android.os.Debug;
import android.os.Process;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

import name.kevinross.tool.AbstractTool;
import name.kevinross.tool.ReflectionUtil;

/**
 * DebuggableTool wraps developer code and handles setting up a debugging environment (if desired)
 * before calling developer code.
 *
 * One should use DebuggableToolHelpers to get the proper invocation for app_process as the methods
 * will take care of the proper parameters and CLASSPATH setup.
 *
 * The specific usage for manually running can be found at DebuggableTool#usage()
 *
 * Configuring debugging after running app_process is a massive hack and will more than likely fail.
 * If it does fail, one will have to rerun with the "-agentlib:[params]" parameter. One should also
 * file a bug report in that case.
 *
 */
public class DebuggableTool {
    /**
     * Call the main method for a class, does sanity checks to make sure it can be loaded
     *
     * Arguments up until "--" tell the runner whether to wait for debugging or other things
     * The first argument after "--" is the fully-qualified name of the main class implementing AbstractTool
     * Every argument after is passed to the tool.
     *
     * @param args
     */
    public static void main(String[] args) {
        List<String> myargs = new ArrayList<>();
        List<String> theirargs = new ArrayList<>();
        String classPath = null;
        boolean willDebug = false;
        if (args.length == 0) {
            usage();
        }

        int argc = -1;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--")) {
                argc = i;
            }
        }

        // 0..argc -> my args. No my args? no-op
        for (int i = 0; i < argc; i++) {
            myargs.add(args[i]);
        }

        // argc starts at -1 so if no myargs, +1 => 0. If myargs, next arg after --
        classPath = args[argc + 1];

        // no flags passed but given -- anyways
        if (classPath.equals("--")) {
            argc += 1;
            classPath = args[argc + 1];
        }

        // grab all args after class_path
        for (int i = argc + 2; i < args.length; i++) {
            theirargs.add(args[i]);
        }

        for (String arg : myargs) {
            if (arg.equals("-D")) {
                willDebug = true;
            }
            if (arg.equals("-MD")) {
                DebuggableToolNative.StartDebugger();
                DebugMyself();
            }
        }

        Class mainClass = null;
        try {
            mainClass = Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            fatal(String.format("Couldn't find the given class name (\"%s\")", classPath));
        }

        if (!mainClass.getSuperclass().equals(AbstractTool.class)) {
            fatal("Tool doesn't extend AbstractTool");
        }

        AbstractTool tool = null;
        try {
            tool = ReflectionUtil.invokes().on(mainClass).of(new Class[]{}).getNewInstance();
        } catch (NoSuchMethodException e) {
            fatal("Tool has no default constructor");
        } catch (IllegalAccessException e) {
            fatal("Tool's default constructor is private");
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            fatal("Unknown error");
        }

        if (willDebug) {
            tool.setWaitForDebugger(true);
            DebuggableToolNative.StartDebugger();
        }
        if (theirargs.size() > 0) {
            tool.setArgs(theirargs.toArray(new String[theirargs.size()]));
        }
        tool.start();
    }

    private static void usage() {
        System.out.println("Usage: \n" +
                "# export CLASSPATH=/path/to/your/apk\n" +
                "# app_process / name.kevinross.tool.debuggable.DebuggableTool [debuggable tool options] -- com.example.cls [arguments]\n" +
                "\n" +
                "where the -- is only needed if parameters for DebuggableTool are needed (for example, to wait for the debugger)");
        System.exit(0);
    }
    private static void fatal(String reason) {
        System.err.println(reason);
        System.exit(1);
    }

    private static void DebugMyself() {
        Process.setArgV0("DebuggableTool");
        ReflectionUtil.invokes().on(android.ddm.DdmHandleAppName.class).
                name("setAppName").
                of(String.class, int.class).
                using("DebuggableTool", UserHandle.myUserId()).
                swallow().invoke();

        Debug.waitForDebugger();
    }
}
