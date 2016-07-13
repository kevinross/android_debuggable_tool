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
    public static long getGRegistryState() {
        return -1;
    }
    public static void StartJdwp() {
    }
    public static void SetJdwpAllowed(boolean allowed) {
    }
    public static void ConfigureJdwp() {
    }
    public static void StartDebugger() {
    }
    public static boolean IsDebuggerConnected() {
        return false;
    }
}
