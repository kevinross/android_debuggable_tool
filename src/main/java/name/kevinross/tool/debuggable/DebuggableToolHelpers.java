package name.kevinross.tool.debuggable;

import android.app.ActivityThread;
import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import dalvik.system.DexFile;
import eu.chainfire.libsuperuser.Shell;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

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
        cmdLine.append(DebuggableTool.class.getName());
        cmdLine.append(" ");
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

    public static Thread runCommandInBackground(boolean su, String command) {
        return runCommandInBackground(su, ActivityThread.currentActivityThread().getApplication().getPackageCodePath(), command);
    }

    public static List<String> runCommand(Context ctx, String command) {
        return runCommand(false, ctx, command);
    }

    public static Thread runCommandInBackground(Context ctx, String command) {
        return runCommandInBackground(false, ctx, command);
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

    public static Thread runCommandInBackground(boolean su, String codePath, String command) {
        return runCommandInBackground(su, 0, codePath, command);
    }

    /**
     * Run a given command with the classpath from a given context
     * @param su should it be run with su?
     * @param uid user ID to run as
     * @param command command string {@see #getCommandLineForMainClass}
     * @return command output
     */
    public static List<String> runCommand(boolean su, int uid, String command) {
        return runCommand(su, uid, ActivityThread.currentActivityThread().getApplication().getPackageCodePath(), command);
    }

    public static Thread runCommandInBackground(boolean su, int uid, String command) {
        return runCommandInBackground(su, uid, ActivityThread.currentActivityThread().getApplication().getPackageCodePath(), command);
    }

    /**
     * Run a given command with the classpath from a given context
     * @param su should it be run with su?
     * @param ctx context for code path
     * @param command command string {@see #getCommandLineForMainClass}
     * @return command output
     */
    public static List<String> runCommand(boolean su, Context ctx, String command) {
        return runCommand(su, 0, ctx.getPackageCodePath(), command);
    }

    public static Thread runCommandInBackground(boolean su, Context ctx, String command) {
        return runCommandInBackground(su, 0, ctx.getPackageCodePath(), command);
    }

    /**
     * Run a given command with the classpath from a given context
     * @param su should it be run with su?
     * @param uid the uid to run under
     * @param ctx context for code path
     * @param command command string {@see #getCommandLineForMainClass}
     * @return command output
     */
    public static List<String> runCommand(boolean su, int uid, Context ctx, String command) {
        return runCommand(su, uid, ctx.getPackageCodePath(), command);
    }

    public static Thread runCommandInBackground(boolean su, int uid, Context ctx, String command) {
        return runCommandInBackground(su, uid, ctx.getPackageCodePath(), command);
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
     * Run a given command with the classpath from a given context
     * @param su should it be run with su?
     * @param uid the uid to run under
     * @param codePath path to dex file
     * @param command command string {@see #getCommandLineForMainClass}
     * @return command output
     */
    public static Thread runCommandInBackground(boolean su, int uid, String codePath, String command) {
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
        // throw it in a thread and return that instead
        final String finalShell = shell;
        final String finalCommand = command;
        final String finalCodePath = codePath;
        final List<String> lines = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                lines.addAll(Shell.run(finalShell, new String[]{finalCommand}, new String[]{String.format("CLASSPATH=%s", finalCodePath)}, true));
            }
        });
        thread.start();
        return thread;
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
        OptionParser parser = new OptionParser("t:p:c:d");
        OptionSet opts = parser.parse(args);
        // <type> [type==script?<dexpath>] <toolclass> <debug> <args...>
        String type = null, dex = null, toolclass = null;
        boolean debug = false;
        List<String> theirargs = new ArrayList<>();

        try {
            type = opts.valueOf("t").toString();
        } catch (Exception ex) {
            type = "cli";
        }

        if (opts.has("p")) {
            dex = opts.valueOf("p").toString();
        }

        try {
            toolclass = opts.valueOf("c").toString();
        } catch (Exception ex) {
            usage();
        }

        if (opts.has("d")) {
            debug = true;
        }

        theirargs.addAll((Collection<? extends String>) opts.nonOptionArguments());

        if (type.equals("cli")) {
            System.out.println(getCommandLineForMainClass(toolclass, debug, theirargs.toArray(new String[theirargs.size()])));
        } else if (type.equals("script")) {
            if (dex == null) {
                usage();
            }
            System.out.println(getScriptForDexAndMainClass(dex, toolclass, debug, theirargs.toArray(new String[theirargs.size()])));
        }
    }
    private static void usage() {
        System.out.println("Usage: DebuggableToolHelpers -t [cli|script] -p [dexpath] -c <toolclass> [-d] <args...");
        System.exit(1);
    }
}
