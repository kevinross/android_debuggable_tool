package name.kevinross.tool.debuggable;


import android.os.Debug;
import android.os.Process;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
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
        if (args.length == 0) {
            usage();
        }
        OptionParser parser = new OptionParser("DF");
        OptionSet opts = parser.parse(args);
        List<?> other = opts.nonOptionArguments();
        if (other.size() == 0) {
            usage();
        }
        List<String> theirargs = new ArrayList<>();
        String classPath = (String)other.get(0);
        theirargs.addAll((Collection<? extends String>) other.subList(1, other.size()));
        boolean willDebug = false;

        if (opts.hasArgument("D")) {
            willDebug = true;
        }
        if (opts.hasArgument("F")) {
            DebuggableToolNative.StartDebugger();
            DebugMyself();
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
