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
extern "C" {
jlong Java_name_kevinross_tool_debuggable_DebuggableToolNative_getGRegistryState(JNIEnv *jni,
                                                                                 jclass self) {
    return (jlong) art::Dbg::gRegistry;
}

void Java_name_kevinross_tool_debuggable_DebuggableToolNative_StartJdwp(JNIEnv *jni, jclass self) {
    art::Dbg::StartJdwp();
}

void Java_name_kevinross_tool_debuggable_DebuggableToolNative_StopJdwp(JNIEnv *jni, jclass self) {
    art::Dbg::StopJdwp();
}
void Java_name_kevinross_tool_debuggable_DebuggableToolNative_ConfigureJdwp(JNIEnv *jni,
                                                                            jclass self) {
    art::JDWP::JdwpOptions opts;
    opts.transport = art::JDWP::kJdwpTransportAndroidAdb;
    opts.server = true;
    art::Dbg::ConfigureJdwp(opts);
}

void Java_name_kevinross_tool_debuggable_DebuggableToolNative_SetJdwpAllowed(JNIEnv *jni,
                                                                             jclass self) {
    art::Dbg::SetJdwpAllowed(true);
}

}
