package name.kevinross.tool;

import android.os.Debug;
import android.os.UserHandle;
import android.os.Process;

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
                using(this.getClass().getSimpleName(), UserHandle.myUserId()).
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
        run(args);
    }
    protected abstract void run(String[] args);
}
