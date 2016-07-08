package name.kevinross.tool.debuggable;

import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.ApplicationThreadNative;
import android.content.Context;
import android.os.Debug;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dalvik.system.DexFile;
import eu.chainfire.libsuperuser.Shell;

/**
 * Helpers useful for developers writing java code interfacing with the android runtime from a root
 * or system context. Takes the need to remember app_process incantations out of the mental working set.
 *
 * The command lines returned are for DebuggableTool executing the given class implementing
 * AbstractTool. There's no reason one couldn't change the code to remove DebuggableTool however one
 * would also need to make sure the passed class has a psv main method app_process could execute.
 */
public class DebuggableToolHelpers {
    /**
     * Get a string suitable to run the given tool on the command line, given appropriate CLASSPATH has been set
     * @param clazz main class implementing AbstractTool
     * @param waitForDebug wait for the debugger
     * @param args arguments for the tool
     * @return command line usable from the shell or chainfire su API
     */
    public static String getCommandLineForMainClass(Class clazz, boolean waitForDebug, String... args) {
        return getCommandLineForMainClass(clazz.getCanonicalName(), waitForDebug, args);
    }
    /**
     * Get a string suitable to run the given tool on the command line, given appropriate CLASSPATH has been set
     * @param clazz main class implementing AbstractTool
     * @param waitForDebug wait for the debugger
     * @param args arguments for the tool
     * @return command line usable from the shell or chainfire su API
     */
    public static String getCommandLineForMainClass(String clazz, boolean waitForDebug, String... args) {
        StringBuilder cmdLine = new StringBuilder();
        cmdLine.append("app_process ");
        cmdLine.append("/ ");
        cmdLine.append("name.kevinross.tool.debuggable.DebuggableTool ");
        if (waitForDebug) {
            cmdLine.append("-D ");
        }
        cmdLine.append("-- ");
        cmdLine.append(clazz);
        for (String a : args) {
            cmdLine.append(String.format(" '%s'", a));
        }
        return cmdLine.toString();
    }

    /**
     * Return a shell script suitable to call the given class in the given dex. If args is empty,
     * args will be $@ and parameters passed to the script will be given to the class
     * @param dexFile
     * @param clazz
     * @param waitForDebug
     * @param args
     * @return
     */
    public static String getScriptForDexAndMainClass(DexFile dexFile, Class clazz, boolean waitForDebug, String... args) {
        return getScriptForDexAndMainClass(dexFile.getName(), clazz.getCanonicalName(), waitForDebug, args);
    }

    /**
     * @see #getScriptForDexAndMainClass(DexFile, Class, boolean, String...)
     * @param dexFile
     * @param clazz
     * @param waitForDebug
     * @param args
     * @return
     */
    public static String getScriptForDexAndMainClass(DexFile dexFile, String clazz, boolean waitForDebug, String... args) {
        return getScriptForDexAndMainClass(dexFile.getName(), clazz, waitForDebug, args);
    }

    /**
     * @see #getScriptForDexAndMainClass(DexFile, Class, boolean, String...)
     * @param dexFile
     * @param clazz
     * @param waitForDebug
     * @param args
     * @return
     */
    public static String getScriptForDexAndMainClass(String dexFile, Class clazz, boolean waitForDebug, String... args) {
        return getScriptForDexAndMainClass(dexFile, clazz.getCanonicalName(), waitForDebug, args);
    }

    /**
     * @see #getScriptForDexAndMainClass(DexFile, Class, boolean, String...)
     * @param dexFile
     * @param clazz
     * @param waitForDebug
     * @param args
     * @return
     */
    public static String getScriptForDexAndMainClass(String dexFile, String clazz, boolean waitForDebug, String... args) {
        StringBuilder out = new StringBuilder("export CLASSPATH=").append(dexFile).append("\n");
        out.append(getCommandLineForMainClass(clazz, waitForDebug, args.length > 0 ? args : new String[]{"$@"})).append("\n");
        return out.toString();
    }

    public static List<String> runCommand(boolean su, String command) {
        return runCommand(su, ActivityThread.currentActivityThread().getApplication().getPackageCodePath(), command);
    }
    /**
     * Run a given command with the classpath from a given context
     * @param su should it be run with su?
     * @param codePath path to dex file
     * @param command command string {@see #getCommandLineForMainClass}
     * @return command output
     */
    public static List<String> runCommand(boolean su, String codePath, String command) {
        return runCommand(su, 0, codePath, command);
    }

    /**
     * Run a given command with the classpath from a given context
     * @param su should it be run with su?
     * @param uid the uid to run under
     * @param codePath path to dex file
     * @param command command string {@see #getCommandLineForMainClass}
     * @return command output
     */
    public static List<String> runCommand(boolean su, int uid, String codePath, String command) {
        String shell = "sh";
        if (su) {
            if (!Shell.SU.available()) {
                throw new RuntimeException("su not available!");
            }
            shell = "su";
        }
        if (uid > 0) {
            command = String.format("%d %s", uid, command);
        }
        return Shell.run(shell, new String[]{command}, new String[]{String.format("CLASSPATH=%s", codePath)}, true);
    }

    /**
     * Get a command-line for a class or a script for a given dex+class, including debug flag and
     * arguments to pass to the code.
     *
     * Usage: [each line is another argument to code, not an actual newline in the shell
     *      <type>      cli or script
     *      {% if type == "script" %}
     *      <dexpath>   path to dex
     *      {% endof %}
     *      <toolclass> fully-qualified classname for tool
     *      <debug>     true|false to enable/disable debugging and waiting for the debugger
     *      <args...>   arguments for the script
     * @param args
     */
    public static void main(String[] args) {
        // <type> [type==script?<dexpath>] <toolclass> <debug> <args...>
        String type = null, dex = null, toolclass = null;
        boolean debug = false;
        // using "int i" loop so can use $i later for theirargs
        int i  = 0;
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                type = args[i];
            } else if (i == 1 && type.equals("script")) {
                dex = args[i];
            } else if ((i == 1 && type.equals("cli")) || (i == 2 && type.equals("script"))) {
                toolclass = args[i];
            } else if (type != null && toolclass != null) {
                debug = Boolean.parseBoolean(args[i]);
                break;
            }
        }
        List<String> arglist = new ArrayList<>();
        for (int j = i + 1; j < args.length; j++) {
            arglist.add(args[j]);
        }
        String[] theirargs = arglist.toArray(new String[arglist.size()]);
        if (type.equals("cli")) {
            System.out.println(getCommandLineForMainClass(toolclass, debug, theirargs));
        } else {
            System.out.println(getScriptForDexAndMainClass(dex, toolclass, debug, theirargs));
        }
    }
}
