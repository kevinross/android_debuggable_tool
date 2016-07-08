package name.kevinross.tool;

import android.app.ActivityThread;
import android.os.Debug;
import android.os.Looper;
import android.os.UserHandle;
import android.os.Process;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Abstract class that facilitates debugging of non-android-app java code. Extend this and
 * implement AbstractTool#run(String[]) as the entry point for tool code.
 */
public abstract class AbstractTool {
    private boolean willWaitForDebugger = false;
    private String[] args = new String[]{};
    public AbstractTool() {
        Process.setArgV0(this.getClass().getName());
        ReflectionUtil.invokes().on(android.ddm.DdmHandleAppName.class).
                name("setAppName").
                of(String.class, int.class).
                using(this.getClass().getName(), UserHandle.myUserId()).
                swallow().invoke();
    }
    public void setWaitForDebugger(boolean willWait) {
        willWaitForDebugger = willWait;
    }
    public void setArgs(String[] args) {
        this.args = args;
    }
    public void start() {
        if (willWaitForDebugger) {
            Debug.waitForDebugger();
        }
        OptionParser parser = getArgParser();
        if (parser != null) {
            run(parser.parse(this.args));
        } else {
            run(args);
        }
    }
    protected abstract void run(String[] args);
    protected void run(OptionSet parser) {
        throw new RuntimeException("subclass must implement this if getArgParser is used");
    }
    protected OptionParser getArgParser() {
        return null;
    }
}
