package name.kevinross.tool.debuggable;


import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import name.kevinross.tool.AbstractTool;
import name.kevinross.tool.R;
import name.kevinross.tool.ReflectionUtil;
import name.kevinross.tool.nativehelpers.NativeToolHelpers;

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
    private static Context ourContext = null;
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
        // set up runtime
        Looper.prepare();
        ActivityThread activityThread = ActivityThread.systemMain();
        Context mSystemContext = activityThread.getSystemContext();
        mSystemContext.setTheme(android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
        try {
            ourContext = mSystemContext.createPackageContext(NativeToolHelpers.getCurrentProcessPackageName(), Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (ourContext == null) {
            System.out.println("context is null");
            return;
        }
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

        if (opts.has("D")) {
            willDebug = true;
        }
        if (opts.has("F")) {
            DebuggableToolNative.StartDebugger();
            DebugMyself();
        }

        Class mainClass = null;
        try {
            mainClass = Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            fatal(String.format(ourContext.getString(R.string.error_class_not_found), classPath));
        }

        if (!mainClass.getSuperclass().equals(AbstractTool.class)) {
            fatal(R.string.error_bad_implementation);
        }

        AbstractTool tool = null;
        try {
            tool = ReflectionUtil.invokes().on(mainClass).of(new Class[]{}).getNewInstance();
        } catch (NoSuchMethodException e) {
            fatal(R.string.error_bad_ctor);
        } catch (IllegalAccessException e) {
            fatal(R.string.error_ctor_visibility);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            fatal(R.string.error_unknown);
        }
        setProcessName(tool.getClass().getName());

        if (willDebug) {
            tool.setWaitForDebugger(true);
            DebuggableToolNative.StartDebugger();
        }
        if (theirargs.size() > 0) {
            tool.setArgs(theirargs.toArray(new String[theirargs.size()]));
        }
        tool.setContext(ourContext);
        tool.start();
    }

    private static void usage() {
        System.out.println(ourContext.getString(R.string.tool_usage));
        System.exit(0);
    }
    private static void fatal(String reason) {
        System.err.println(reason);
        System.exit(1);
    }
    private static void fatal(int reasoncode) {
        System.err.println(ourContext.getString(reasoncode));
        System.exit(1);
    }

    private static void DebugMyself() {
        setProcessName("DebuggableTool");
        Debug.waitForDebugger();
    }

    private static void setProcessName(String cls) {
        Process.setArgV0(cls);
        ReflectionUtil.invokes().on(android.ddm.DdmHandleAppName.class).
                name("setAppName").
                of(String.class, int.class).
                using(cls, UserHandle.myUserId()).
                swallow().invoke();
    }
}
