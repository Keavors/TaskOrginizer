// JNI bridge between Kotlin (WhisperNative) and whisper.cpp.
#include <android/log.h>
#include <jni.h>

#include <atomic>
#include <string>

#include "ggml-backend.h"
#include "whisper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct Session {
    whisper_context *ctx = nullptr;
    std::atomic<bool> abort{false};
};

void log_to_logcat(ggml_log_level level, const char *text, void *) {
    if (level == GGML_LOG_LEVEL_DEBUG) return;
    const int prio = level == GGML_LOG_LEVEL_ERROR  ? ANDROID_LOG_ERROR
                     : level == GGML_LOG_LEVEL_WARN ? ANDROID_LOG_WARN
                                                    : ANDROID_LOG_INFO;
    __android_log_print(prio, TAG, "%s", text);
}

std::string to_string(JNIEnv *env, jstring s) {
    if (s == nullptr) return {};
    const char *chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_keavors_taskorganizer_speech_WhisperNative_initBackends(JNIEnv *env, jobject, jstring libDir) {
    whisper_log_set(log_to_logcat, nullptr);
    const std::string dir = to_string(env, libDir);
    // Loads the best CPU backend variant for this phone (libggml-cpu-android_*.so).
    ggml_backend_load_all_from_path(dir.c_str());
    LOGI("backends loaded from %s, devices: %zu", dir.c_str(), ggml_backend_dev_count());
}

JNIEXPORT jstring JNICALL
Java_com_keavors_taskorganizer_speech_WhisperNative_systemInfo(JNIEnv *env, jobject) {
    std::string info = whisper_print_system_info();
    for (size_t i = 0; i < ggml_backend_dev_count(); i++) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        info += " | device: ";
        info += ggml_backend_dev_name(dev);
        ggml_backend_reg_t reg = ggml_backend_dev_backend_reg(dev);
        if (reg) {
            info += " (";
            info += ggml_backend_reg_name(reg);
            info += ")";
        }
    }
    return env->NewStringUTF(info.c_str());
}

JNIEXPORT jlong JNICALL
Java_com_keavors_taskorganizer_speech_WhisperNative_initContext(JNIEnv *env, jobject, jstring modelPath) {
    const std::string path = to_string(env, modelPath);
    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    params.flash_attn = true;
    whisper_context *ctx = whisper_init_from_file_with_params(path.c_str(), params);
    if (ctx == nullptr) {
        LOGE("failed to load model %s", path.c_str());
        return 0;
    }
    auto *session = new Session();
    session->ctx = ctx;
    return reinterpret_cast<jlong>(session);
}

JNIEXPORT void JNICALL
Java_com_keavors_taskorganizer_speech_WhisperNative_freeContext(JNIEnv *, jobject, jlong handle) {
    auto *session = reinterpret_cast<Session *>(handle);
    if (session == nullptr) return;
    whisper_free(session->ctx);
    delete session;
}

JNIEXPORT void JNICALL
Java_com_keavors_taskorganizer_speech_WhisperNative_requestAbort(JNIEnv *, jobject, jlong handle) {
    auto *session = reinterpret_cast<Session *>(handle);
    if (session != nullptr) session->abort.store(true);
}

/**
 * Recognizes mono 16 kHz PCM. Returns UTF-8 bytes of the text (decoded in Kotlin,
 * which tolerates emoji and broken sequences), or null when cancelled or failed.
 * audioCtx = 0 uses the full 30 s encoder window; a smaller value is faster for short clips.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_keavors_taskorganizer_speech_WhisperNative_transcribe(JNIEnv *env, jobject, jlong handle, jfloatArray pcm,
                                                               jstring language, jstring prompt, jstring vadModelPath,
                                                               jint threads, jint audioCtx) {
    auto *session = reinterpret_cast<Session *>(handle);
    if (session == nullptr || session->ctx == nullptr) return nullptr;
    session->abort.store(false);

    const std::string lang = to_string(env, language);
    const std::string initial_prompt = to_string(env, prompt);
    const std::string vad_path = to_string(env, vadModelPath);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = threads > 0 ? threads : 4;
    params.language = lang.empty() ? "ru" : lang.c_str();
    params.detect_language = lang == "auto";
    params.translate = false;
    params.no_context = true;
    params.no_timestamps = true;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_special = false;
    params.print_timestamps = false;
    params.temperature = 0.0f;
    params.temperature_inc = 0.2f;
    params.audio_ctx = audioCtx;
    if (!initial_prompt.empty()) {
        params.initial_prompt = initial_prompt.c_str();
        params.carry_initial_prompt = true;
    }
    if (!vad_path.empty()) {
        params.vad = true;
        params.vad_model_path = vad_path.c_str();
        params.vad_params = whisper_vad_default_params();
        params.vad_params.speech_pad_ms = 200;
    }
    params.abort_callback = [](void *user_data) {
        return static_cast<Session *>(user_data)->abort.load();
    };
    params.abort_callback_user_data = session;

    const jsize n = env->GetArrayLength(pcm);
    jfloat *samples = env->GetFloatArrayElements(pcm, nullptr);
    const int rc = whisper_full(session->ctx, params, samples, n);
    env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);

    if (rc != 0 || session->abort.load()) {
        if (rc != 0) LOGE("whisper_full failed: %d", rc);
        return nullptr;
    }

    std::string text;
    const int segments = whisper_full_n_segments(session->ctx);
    for (int i = 0; i < segments; i++) {
        text += whisper_full_get_segment_text(session->ctx, i);
    }
    jbyteArray out = env->NewByteArray(static_cast<jsize>(text.size()));
    env->SetByteArrayRegion(out, 0, static_cast<jsize>(text.size()), reinterpret_cast<const jbyte *>(text.data()));
    return out;
}

}  // extern "C"
