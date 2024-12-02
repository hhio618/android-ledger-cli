#include <jni.h>

#include <exception>
#include <stdexcept>

#include <ledger_wrap/session.h>
#include <ledger_wrap/global.h>
#include <string>
#include <sstream>
#include <iostream>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include "exceptions.h"

static inline jlong to_jlong(ledger_wrap::session_ptr session)
{
    return reinterpret_cast<jlong>(session.as_ptr());
}

static inline ledger_wrap::session_ptr from_jlong(jlong session)
{
    return ledger_wrap::session_ptr(reinterpret_cast<ledger::session_t*>(session));
}


extern "C" JNIEXPORT jstring JNICALL
Java_net_ilammy_ledger_api_Global_runCommand(JNIEnv* env, jclass klass, jstring jcommand) {
    const char* commandCStr = env->GetStringUTFChars(jcommand, nullptr);
    char commandOutput[8192]; // Static buffer for captured output
    memset(commandOutput, 0, sizeof(commandOutput));

    // Create a pipe for capturing stdout
    int pipefd[2];
    if (pipe(pipefd) == -1) {
        env->ReleaseStringUTFChars(jcommand, commandCStr);
        return env->NewStringUTF("Error: Failed to create pipe.");
    }

    // Backup the original stdout and stderr
    int originalStdout = dup(STDOUT_FILENO);
    int originalStderr = dup(STDERR_FILENO);
    if (originalStdout == -1 || originalStderr == -1) {
        close(pipefd[0]);
        close(pipefd[1]);
        env->ReleaseStringUTFChars(jcommand, commandCStr);
        return env->NewStringUTF("Error: Failed to duplicate stdout or stderr.");
    }

    // Redirect stdout and stderr to the pipe
    if (dup2(pipefd[1], STDOUT_FILENO) == -1 || dup2(pipefd[1], STDERR_FILENO) == -1) {
        close(pipefd[0]);
        close(pipefd[1]);
        close(originalStdout);
        close(originalStderr);
        env->ReleaseStringUTFChars(jcommand, commandCStr);
        return env->NewStringUTF("Error: Failed to redirect stdout or stderr.");
    }
    close(pipefd[1]); // Close write-end of the pipe in the parent process

    // Parse the command string into argc and argv
    char* args[64]; // Static array for arguments (supports up to 64 arguments)
    int argc = 0;

    char* token = strtok(const_cast<char*>(commandCStr), " ");
    while (token != NULL && argc < 64) {
        args[argc++] = token;
        token = strtok(NULL, " ");
    }
    char* envp[] = {};
    // Call Ledger's main function
    int returnCode = ledger_wrap::call_main(argc, args, envp);

    // Read the output from the pipe
    ssize_t bytesRead = read(pipefd[0], commandOutput, sizeof(commandOutput) - 1);
    if (bytesRead > 0) {
        commandOutput[bytesRead] = '\0'; // Null-terminate the output
    } else {
        snprintf(commandOutput, sizeof(commandOutput), "Error: No output captured.");
    }

    // Restore stdout and stderr
    dup2(originalStdout, STDOUT_FILENO);
    dup2(originalStderr, STDERR_FILENO);
    close(originalStdout);
    close(originalStderr);
    close(pipefd[0]);

    // Append error information if the command failed
    if (returnCode != 0) {
        char errorOutput[256];
        snprintf(errorOutput, sizeof(errorOutput), "Error: Command failed with code %d\n", returnCode);
        strncat(commandOutput, errorOutput, sizeof(commandOutput) - strlen(commandOutput) - 1);
    }

    // Release the Java string
    env->ReleaseStringUTFChars(jcommand, commandCStr);

    // Return the captured output to Java
    return env->NewStringUTF(commandOutput);
}

extern "C" JNIEXPORT
jlong JNICALL Java_net_ilammy_ledger_api_Session_newSession(JNIEnv *env, jclass klass)
{
    try {
        auto session = ledger_wrap::session_ptr::make();
        return to_jlong(session);
    }
    catch (const std::exception &e) {
        ledger_jni::rethrow_as_java_runtime_exception(env, e);
        return 0;
    }
}

extern "C" JNIEXPORT
void JNICALL Java_net_ilammy_ledger_api_Session_deleteSession(JNIEnv *env, jclass klass, jlong sessionPtr)
{
    try {
        auto session = from_jlong(sessionPtr);
        ledger_wrap::session_ptr::free(session);
    }
    catch (const std::exception &e) {
        ledger_jni::rethrow_as_java_runtime_exception(env, e);
    }
}

extern "C" JNIEXPORT
void JNICALL Java_net_ilammy_ledger_api_Session_readJournalFromString(JNIEnv *env, jclass klass, jlong sessionPtr, jbyteArray data)
{
    try {
        auto session = from_jlong(sessionPtr);
        if (!session) {
            throw std::invalid_argument("sessionPtr cannot be null");
        }

        // Copy Java byte array into a string.
        auto dataLen = env->GetArrayLength(data);
        std::string dataString(dataLen, 0);
        {
            auto bytes = env->GetPrimitiveArrayCritical(data, nullptr);
            if (!bytes) {
                return; // Java exception already thrown
            }
            dataString.assign(static_cast<const char*>(bytes), dataLen);
            env->ReleasePrimitiveArrayCritical(data, bytes, JNI_ABORT);
        }

        session->read_journal_from_string(dataString);
    }
    catch (const std::exception &e) {
        ledger_jni::rethrow_as_java_runtime_exception(env, e);
    }
}

static inline jlong to_jlong(ledger_wrap::journal_ptr journal)
{
    return reinterpret_cast<jlong>(journal.as_ptr());
}

extern "C" JNIEXPORT
jlong JNICALL Java_net_ilammy_ledger_api_Session_getJournal(JNIEnv *env, jclass klass, jlong sessionPtr)
{
    try {
        auto session = from_jlong(sessionPtr);
        if (!session) {
            throw std::invalid_argument("sessionPtr cannot be null");
        }

        auto journal = session->get_journal();
        return to_jlong(journal);
    }
    catch (const std::exception &e) {
        ledger_jni::rethrow_as_java_runtime_exception(env, e);
        return 0;
    }
}
