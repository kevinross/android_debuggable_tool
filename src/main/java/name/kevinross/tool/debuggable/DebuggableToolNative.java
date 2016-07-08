package name.kevinross.tool.debuggable;

import name.kevinross.tool.ReflectionUtil;
import name.kevinross.tool.nativehelpers.NativeToolHelpers;

/**
 * Allow for post-process-create debugger configuration. Here there be dragons.
 *
 * This depends very strongly on the current art::Jdwp and art::Dbg interfaces in the current ART
 * source tree.
 *
 * Call DebuggableToolNative#StartDebugger() after doing DdmHandleAppName#setAppname(String) to
 * allow for attaching to the process with any compliant JDWP debugger.
 */
public class DebuggableToolNative {
    static {
        NativeToolHelpers.injectNativeLibraryPath();
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("DebuggableToolNative");
    }
    public static native long getGRegistryState();
    public static native void StartJdwp();
    public static native void SetJdwpAllowed(boolean allowed);
    public static native void ConfigureJdwp();
    public static void StartDebugger() {
        if (IsDebuggerConnected()) {
            System.out.println("debugger already connected");
            return;
        }
        // in the event that isDebuggerConnected fails: all of this machinery will have already been created
        // and an assert will catch that, killing the process. Make sure the current gRegistry is 0 (aka: nullptr)
        // otherwise bail out
        if (getGRegistryState() != 0) {
            System.out.println("registry already created");
            return;
        }
        SetJdwpAllowed(true);
        ConfigureJdwp();
        StartJdwp();
    }
    public static boolean IsDebuggerConnected() {
        Class vmDebug = ReflectionUtil.getClassByName(ClassLoader.getSystemClassLoader(), "dalvik.system.VMDebug");
        return ReflectionUtil.invokes().on(vmDebug).name("isDebuggerConnected").swallow().<Boolean>invoke();
    }
}
