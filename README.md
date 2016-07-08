# Debuggable CLI Tools

This repository provides convenience functions and machinery for invoking java code on the android platform outside of the standard app runtime and hooks into the android runtime itself to set up debugging so one can attach to the process from a JDWP-compliant debugger (read: Android Studio et al).

The idea is that one can use the standard tools and the standard framework to write and debug code running in a subprocess, a subprocess that more than likely is a root process.

## Usage

### Setup

Copy `libart.so` from your device to `${ndk_path}/platforms/android-${sdk_version}/arch-${arch}/usr/lib` where `${ndk_path}` is the root of the ndk SDK, `${arch}` is the processor architecture (as defined by the ndk) ("arm" and "arm64" are typical for recent devices and one should copy both from `/system/lib` and `/system/lib64` to their respective paths), and `${sdk_version}` is the SDK version (22, 23, 24, etc).

### Implementation

All one has to do is extend `AbstractTool` and implement `AbstractTool#run(String[])` to get the same behaviour as regular android apps with respect to debugging.

### Execution

There is the unavoidable one-time bootstrap of the tool's convenience script: running `app_process` on a tool will require setting a CLASSPATH which will therefore require finding the dex path for the app and setting CLASSPATH in a shell: `export CLASSPATH=/data/app/name.of.app/base.apk`.

Once one has the environment (read: CLASSPATH) for one's app, running `app_process / name.kevinross.tool.debuggable.DebuggableToolHelpers [args]` will give what one wants for running code from one's app. There are several options:

1) Use `SH.run`, pass the appropriate CLASSPATH in the environment parameter, and use the output of `DebuggableToolHelpers "cli" "classname" "debug" [args]` as the command.
2) Use `DebuggableToolHelpers#getCommandLineForMainClass` and `DebuggableToolHelpers#runCommand` (given the path to the dex file)
2) Use `SH.run` and pass `script` instead of `cli` as in (1), pass the path to the dex before the classname, then use the rest of the params in (1), and write the output to some location one can read from and that one can call a script from (excluding any other environment variables and parameters. Make sure that new installs of one's app won't affect the CLASSPATH: packagemanager switches the install directories on each install (from `-1` to `-2` cyclically) so the path to the script shouldn't depend on the path in `/data/app`).

## Debugging

Run the `app_process` directly (with appropriate parameters) or shell script (as previously generated) with the debugging flag. Once the program has started and the console displays `Sending WAIT chunk`, use the "attach to process" feature of the IDE to debug.