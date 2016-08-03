package name.kevinross.tool;

import android.app.ActivityThread;
import android.content.Context;
import android.os.Debug;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import name.kevinross.tool.debuggable.DebuggableToolHelpers;

/**
 * Abstract class that facilitates debugging of non-android-app java code. Extend this and
 * implement AbstractTool#run(String[]) or AbstractTool#run(OptionSet) as the entry point for tool
 * code. You must not have a default constructor: this particular mechanism means anything done in
 * the default ctor app-side won't have any use on the tool-side as they will be different instances
 * in different processes.
 *
 * To run your code, instantiate your class and call any of the runTool(*) methods.
 *
 *      new YourTool().runTool("hello", "world");
 *      new YourTool().runTool("--flag", "param");
 *
 * To debug your code, a "builder" mechanism is used:
 *
 *      new YourTool().setWaitForDebugger(true).runTool("hello", "world");
 *
 */
public abstract class AbstractTool {
    private boolean willWaitForDebugger = false;
    private String[] args = new String[]{};
    private ActivityThread thisActivityThread = null;
    private Context thisContext = null;
    protected OptionSet parsedArgs = null;

    /**
     * Get the context obtained via PackageManager inspecting the containing package
     * @return
     */
    protected Context getContext() {
        return thisContext;
    }

    /**
     * Get the activity thread for the current process
     * @return
     */
    protected ActivityThread getActivityThread() {
        return thisActivityThread;
    }

    /**
     * In client code, instantiate the class and call #runTool(*) or #runSuTool(*) to run code.
     */
    public AbstractTool() {

    }

    private String getCommandLine(String... args) {
        return DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args);
    }

    /**
     * Run the tool in a separate process with the given arguments
     * @param args
     * @return
     */
    public List<String> runTool(String... args) {
        return DebuggableToolHelpers.runCommand(false, getCommandLine(args));
    }

    public Thread runService(String... args) {
        return DebuggableToolHelpers.runCommandInBackground(false, getCommandLine(args));
    }

    /**
     * Run the tool in a separate process with the given arguments and context
     * @param ctx
     * @param args
     * @return
     */
    public List<String> runTool(Context ctx, String... args) {
        return DebuggableToolHelpers.runCommand(false, ctx, getCommandLine(args));
    }

    public Thread runService(Context ctx, String... args) {
        return DebuggableToolHelpers.runCommandInBackground(false, ctx, getCommandLine(args));
    }

    /**
     * Run the tool in a separate process with the given arguments
     * @param su run as root
     * @param args
     * @return
     */
    public List<String> runTool(boolean su, String... args) {
        return DebuggableToolHelpers.runCommand(su, getCommandLine(args));
    }

    public Thread runService(boolean su, String... args) {
        return DebuggableToolHelpers.runCommandInBackground(su, getCommandLine(args));
    }

    /**
     * Run the tool as root with the given context and arguments
     * @param su run as root
     * @param ctx
     * @param args
     * @return
     */
    public List<String> runTool(boolean su, Context ctx, String... args) {
        return DebuggableToolHelpers.runCommand(su, ctx, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
    }

    public Thread runService(boolean su, Context ctx, String... args) {
        return DebuggableToolHelpers.runCommandInBackground(su, ctx, getCommandLine(args));
    }

    /**
     * Run the tool as $uid with the given arguments (requires root to obtain $uid)
     * @param uid
     * @param args
     * @return
     */
    public List<String> runTool(int uid, String... args) {
        return DebuggableToolHelpers.runCommand(true, uid, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
    }

    public Thread runService(int uid, String... args) {
        return DebuggableToolHelpers.runCommandInBackground(true, uid, getCommandLine(args));
    }

    /**
     * Run the tool as $uid with the given context and arguments (requires root to obtain $uid)
     * @param uid
     * @param ctx
     * @param args
     * @return
     */
    public List<String> runTool(int uid, Context ctx, String... args) {
        return DebuggableToolHelpers.runCommand(true, uid, ctx, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
    }

    public Thread runService(int uid, Context ctx, String... args) {
        return DebuggableToolHelpers.runCommandInBackground(true, uid, ctx, getCommandLine(args));
    }

    /**
     * Wait for the debugger to attach before running the tool's #run() method
     * @param willWait
     * @return
     */
    public <I extends AbstractTool> I setWaitForDebugger(boolean willWait) {
        willWaitForDebugger = willWait;
        return (I)this;
    }
    public void setArgs(String[] args) {
        this.args = args;
        this.parsedArgs = getArgParser().parse(args);
    }
    protected String[] getArgs() {
        return args;
    }
    public void setContext(Context ctx) {
        thisContext = ctx;
    }
    public void setActivityThread(ActivityThread thread) {thisActivityThread = thread;}
    public void start() {
        if (willWaitForDebugger) {
            Debug.waitForDebugger();
        }
        run(parsedArgs);
    }
    /**
     * Implement this in client code as the main entry point
     * @param parser
     */
    protected abstract void run(OptionSet parser);

    /**
     * Implementing this to allow for validating parameters
     * @return
     */
    protected OptionParser getArgParser() {
        return new OptionParser();
    }

    /**
     * Implement this to override the process namd
     * @return
     */
    public String getAppName() {
        return getClass().getName();
    }
}
