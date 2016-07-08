# Debuggable CLI Tools

This repository provides convenience functions and machinery for invoking java code on the android platform outside of the standard app runtime and hooks into the android runtime itself to set up debugging so you can attach to the process from a JDWP-compliant debugger (read: Android Studio et al).

The idea is that you can use the standard tools and the standard framework to write and debug code running in a subprocess, a subprocess that more than likely is a root process.

## Usage

### Setup

Copy `libart.so` from your device to `${ndk_path}/platforms/android-${sdk_version}/arch-${arch}/usr/lib` where `${ndk_path}` is the root of the ndk SDK, `${arch}` is the processor architecture (as defined by the ndk) ("arm" and "arm64" are typical for recent devices and you should copy both from `/system/lib` and `/system/lib64` to their respective paths), and `${sdk_version}` is the SDK version (22, 23, 24, etc). There's no way around this requirement: a shared library will be built that accesses functions in it (I tried adding `-Wl,--unresolved-symbols=ignore-all` to the linker flags however the app + tool would crash when `DebuggableToolNative.<clinit>` gets invoked due to unresolvable symbols).

### Implementation

All you have to do is extend `AbstractTool` and implement `AbstractTool#run(String[])` to get the same behaviour as regular android apps with respect to debugging.

### Execution

There is the unavoidable one-time bootstrap of the tool's convenience script: running `app_process` on a tool will require setting a CLASSPATH which will therefore require finding the dex path for the app and setting CLASSPATH in a shell: `export CLASSPATH=/data/app/name.of.app/base.apk`.

Once you have the environment (read: CLASSPATH) for your app, running `app_process / name.kevinross.tool.debuggable.DebuggableToolHelpers [args]` will give what you need for running code from your app. There are several options:

1) Use `SH.run`, pass the appropriate CLASSPATH in the environment parameter, and use the output of `DebuggableToolHelpers  -t "cli" -c "classname" -d -- [args]` as the command.
2) Use `DebuggableToolHelpers#getCommandLineForMainClass` and `DebuggableToolHelpers#runCommand` (given the path to the dex file)
3) Use `SH.run` and pass `script` instead of `cli` as in (1), pass the path to the dex using `-p [path]`, then use the rest of the params in (1), and write the output to some location you can read from and that you can call a script from (excluding any other environment variables and parameters. Make sure that new installs of your app won't affect the CLASSPATH: packagemanager switches the install directories on each install (from `-1` to `-2` cyclically) so the path to the script shouldn't depend on the path in `/data/app`).

## Debugging

Run the `app_process` directly (with appropriate parameters) or shell script (as previously generated) with the debugging flag. Once the program has started and the console displays `Sending WAIT chunk`, use the "attach to process" feature of the IDE to debug.

## Example

Sample tool:
```
package demo.tool;
class MyTool extends AbstractTool {
    public void run(String[] args) {
        for (String arg : args) {
            System.out.println(arg);
        }
    }
}
```

Sample invocation to for tool `demo.tool.MyTool`:

```
export CLASSPATH=/data/app/demo.tool.mytoolapp-1/base.apk
# no debugging here
app_process / name.kevinross.tool.debuggable.DebuggableTool demo.tool.MyTool hello world
# debug the tool (have a breakpoint set on MyTool#run otherwise the tool will run as 
# normal (and probably exit) once the IDE connects
app_process / name.kevinross.tool.debuggable.DebuggableTool -D -- demo.tool.MyTool hello world
```

## Can't instantiate your tool class?

You can debug `DebuggableTool` itself by passing `-MD` instead of `-D` and setting a breakpoint on `Class mainClass = null;` in `DebuggableTool.java`.

## Find a bug?

Follow generally accepted bug-reporting practices: search for possible existing bugs, minimal test case, etc.

