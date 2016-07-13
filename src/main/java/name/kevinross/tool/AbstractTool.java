package name.kevinross.tool;

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
    private Context thisContext = null;

    /**
     * Get the context obtained via PackageManager inspecting the containing package
     * @return
     */
    protected Context getContext() {
        return thisContext;
    }

    /**
     * In client code, instantiate the class and call #runTool(*) or #runSuTool(*) to run code.
     */
    public AbstractTool() {

    }

    /**
     * Run the tool in a separate process with the given arguments
     * @param args
     * @return
     */
    public List<String> runTool(String... args) {
        return DebuggableToolHelpers.runCommand(false, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
    }

    /**
     * Run the tool in a separate process with the given arguments and context
     * @param ctx
     * @param args
     * @return
     */
    public List<String> runTool(Context ctx, String... args) {
        return DebuggableToolHelpers.runCommand(false, ctx, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
    }

    /**
     * Run the tool in a separate process with the given arguments
     * @param su run as root
     * @param args
     * @return
     */
    public List<String> runTool(boolean su, String... args) {
        return DebuggableToolHelpers.runCommand(su, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
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

    /**
     * Run the tool as $uid with the given arguments (requires root to obtain $uid)
     * @param uid
     * @param args
     * @return
     */
    public List<String> runTool(int uid, String... args) {
        return DebuggableToolHelpers.runCommand(true, uid, DebuggableToolHelpers.getCommandLineForMainClass(this.getClass(), willWaitForDebugger, args));
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

    /**
     * Wait for the debugger to attach before running the tool's #run() method
     * @param willWait
     * @return
     */
    public AbstractTool setWaitForDebugger(boolean willWait) {
        willWaitForDebugger = willWait;
        return this;
    }
    public void setArgs(String[] args) {
        this.args = args;
    }
    protected String[] getArgs() {
        List<?> args = getArgParser().parse(this.args).nonOptionArguments();
        return args.toArray(new String[args.size()]);
    }
    public void setContext(Context ctx) {
        thisContext = ctx;
    }
    public void start() {
        if (willWaitForDebugger) {
            Debug.waitForDebugger();
        }
        OptionParser parser = getArgParser();
        run(parser.parse(this.args));
    }

    /**
     * Implement #run(String[]) or #run(OptionSet) in client code
     * @param parser
     */
    protected abstract void run(OptionSet parser);

    /**
     * Implementing this will trigger the #run(OptionSet) version of code
     * @return
     */
    protected OptionParser getArgParser() {
        return new OptionParser();
    }
}
