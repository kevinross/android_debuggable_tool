package name.kevinross.tool.nativehelpers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.PathClassLoader;
import name.kevinross.tool.ReflectionUtil;

/**
 * Created by kevinross (contact@kevinross.name) on 2016-07-05.
 */
public class NativeToolHelpers {
    /**
     * Wrapper for internal VMRuntime class that exposes the current runtime instruction set
     */
    public static class VMRuntime {
        public static String getCurrentInstructionSet() {
            // get the internal VMRuntime class
            Class VMRuntimeClass = ReflectionUtil.getClassByName(ClassLoader.getSystemClassLoader(), "dalvik.system.VMRuntime");
            // and the instruction set of the running process
            return ReflectionUtil.invokes().on(VMRuntimeClass).name("getCurrentInstructionSet").nosy().swallow().<String>invoke();
        }
    }

    /**
     * Get the installation path of the android package the tool is a part of (eg "/data/app/name.kevinross.demotool-1")
     * @return
     */
    public static String getCurrentProcessPackagePath() {
        try {
            // get the current process' classes.dex
            String classes_dex = new URL(Thread.currentThread().getContextClassLoader().getResource("classes.dex").getFile()).getFile().replace("!/classes.dex", "");
            // now the parent dir
            return new File(classes_dex).getParentFile().getAbsolutePath().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the android package name for the current process
     * @return
     */
    public static String getCurrentProcessPackageName() {
        return new File(getCurrentProcessPackagePath()).getName().replace("-1", "").replace("-2", "");
    }

    /**
     * Inject the path to native libraries in the current android package into the classloader
     */
    public static void injectNativeLibraryPath() {
        List<String> paths = new ArrayList<>();
        String currentPackagePath = getCurrentProcessPackagePath();
        injectNativeLibraryPath(currentPackagePath + "/lib/" + VMRuntime.getCurrentInstructionSet());
    }

    /**
     * Inject an arbitrary path for native libraries into the classloader
     * @param path
     */
    public static void injectNativeLibraryPath(String path) {
        injectNativeLibraryPath(new String[]{path});
    }
    /**
     * Inject arbitrary paths for native libraries into the classloader
     * @param paths
     */
    public static void injectNativeLibraryPath(String[] paths) {
        /* see libcore source for details on fields, suffice it to say, this is the basic structure:
        class PathClassLoader < DexClassLoader < BaseDexClassLoader
        class BaseDexClassLoader {
            private final DexPathList pathList;
        }
        class DexPathList {
            private final Element[] nativeLibraryPathElements;
            private final List<String> nativeLibraryDirectories;
            private final List<String> systemNativeLibraryDirectories;
            public DexPathList(Classloader, String dexPath, String librarySearchPath, File optimizedDirectory) {
                // add dex to classpath, merge dex's natives with system natives and add to nativeLibrarySearchPath
            }
        }

        and why this needs to happen: running app_process manually results in the "dex" being the system framework
        and not the containing dex. Therefore, the librarySearchPath will only be the system's. Add this class' to
        the path with this function.
         */
        // get the classloader
        PathClassLoader cl = (PathClassLoader) PathClassLoader.getSystemClassLoader();
        // get the path list
        Object dexPathList = ReflectionUtil.invokes().on(cl).name("pathList").nosy().swallow().get();
        // get the current nativeLibraryDirectories
        List<File> nativeLibraryDirectories = ReflectionUtil.invokes().on(dexPathList).name("nativeLibraryDirectories").nosy().swallow().<List<File>>get();
        // get the system natives
        List<File> systemNativeLibraryDirectories = ReflectionUtil.invokes().on(dexPathList).name("systemNativeLibraryDirectories").nosy().swallow().<List<File>>get();
        // add the requested paths
        for (String path : paths) {
            systemNativeLibraryDirectories.add(new File(path));
        }
        // merge the existing natives and the system natives
        List<File> allNativeDirs = new ArrayList<>(nativeLibraryDirectories);
        allNativeDirs.addAll(systemNativeLibraryDirectories);
        // get the path elements for this list
        Object pathElements = ReflectionUtil.invokes().
                on(dexPathList).name("makePathElements").of((Class) List.class, File.class, (Class) List.class).
                using(allNativeDirs, null, new ArrayList<IOException>()).nosy().swallow().invoke();
        // replace the existing elements
        ReflectionUtil.invokes().on(dexPathList).name("nativeLibraryPathElements").using(pathElements, null).nosy().swallow().set();
    }
}
