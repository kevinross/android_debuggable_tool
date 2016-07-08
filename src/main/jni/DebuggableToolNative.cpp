//
// Created by Kevin Ross on 2016-07-02.
//
#include <memory>
#include <jni.h>
#include <string>
#include <stdint.h>
#include <iostream>
#include <debugger.h>
#include <jdwp/jdwp.h>
/*namespace art {
    namespace JDWP {
        enum JdwpTransportType {
            kJdwpTransportUnknown = 0,
            kJdwpTransportSocket,
            kJdwpTransportAndroidAdb
        };
        struct JdwpOptions {
            JdwpTransportType transport = kJdwpTransportUnknown;
            bool server = false;
            bool suspend = false;
            std::string host = "";
            uint16_t port = static_cast<uint16_t>(-1);
        };
    }
    class Dbg {
    public:
        static void SetJdwpAllowed(bool allowed);
        static void StartJdwp();
        static void StopJdwp();
        static void ConfigureJdwp(const JDWP::JdwpOptions& jdwp_options);
        static bool IsJdwpConfigured();
    };
}*/
extern "C" {
jlong Java_name_kevinross_tool_debuggable_DebuggableToolNative_getGRegistryState(JNIEnv *jni,
                                                                                 jclass self) {
    return (jlong) art::Dbg::gRegistry;
}

void Java_name_kevinross_tool_debuggable_DebuggableToolNative_StartJdwp(JNIEnv *jni, jobject self) {
    art::Dbg::StartJdwp();
}

void Java_name_kevinross_tool_debuggable_DebuggableToolNative_StopJdwp(JNIEnv *jni, jobject self) {
    art::Dbg::StopJdwp();
}
void Java_name_kevinross_tool_debuggable_DebuggableToolNative_ConfigureJdwp(JNIEnv *jni,
                                                                            jobject self) {
    art::JDWP::JdwpOptions opts;
    opts.transport = art::JDWP::kJdwpTransportAndroidAdb;
    opts.server = true;
    art::Dbg::ConfigureJdwp(opts);
}

void Java_name_kevinross_tool_debuggable_DebuggableToolNative_SetJdwpAllowed(JNIEnv *jni,
                                                                             jobject self) {
    art::Dbg::SetJdwpAllowed(true);
}

jboolean Java_name_kevinross_tool_debuggable_DebuggableToolNative_IsDebuggerActive(JNIEnv *jni,
                                                                                   jobject self) {
    return art::Dbg::IsDebuggerActive();
}
}
