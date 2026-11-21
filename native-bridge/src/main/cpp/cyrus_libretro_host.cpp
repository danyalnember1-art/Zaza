#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>
#include <string>
#include <vector>
#include <mutex>
#include <fstream>
#include <algorithm>
#include "libretro.h"

#define LOG_TAG "CyrusLibretro"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct CoreSymbols {
    void (*retro_init)(void) = nullptr;
    void (*retro_deinit)(void) = nullptr;
    unsigned (*retro_api_version)(void) = nullptr;
    void (*retro_get_system_info)(struct retro_system_info *) = nullptr;
    void (*retro_get_system_av_info)(struct retro_system_av_info *) = nullptr;
    void (*retro_set_environment)(retro_environment_t) = nullptr;
    void (*retro_set_video_refresh)(retro_video_refresh_t) = nullptr;
    void (*retro_set_audio_sample)(retro_audio_sample_t) = nullptr;
    void (*retro_set_audio_sample_batch)(retro_audio_sample_batch_t) = nullptr;
    void (*retro_set_input_poll)(retro_input_poll_t) = nullptr;
    void (*retro_set_input_state)(retro_input_state_t) = nullptr;
    void (*retro_set_controller_port_device)(unsigned port, unsigned device) = nullptr;
    void (*retro_reset)(void) = nullptr;
    void (*retro_run)(void) = nullptr;
    size_t (*retro_serialize_size)(void) = nullptr;
    bool (*retro_serialize)(void *data, size_t size) = nullptr;
    bool (*retro_unserialize)(const void *data, size_t size) = nullptr;
    bool (*retro_load_game)(const struct retro_game_info *game) = nullptr;
    void (*retro_unload_game)(void) = nullptr;
    void *(*retro_get_memory_data)(unsigned id) = nullptr;
    size_t (*retro_get_memory_size)(unsigned id) = nullptr;
};

struct LibretroInstance {
    void *handle = nullptr;
    CoreSymbols sym{};
    std::string systemDir;
    std::string saveDir;
    std::string sramPath;
    int pixelFormat = RETRO_PIXEL_FORMAT_0RGB1555;
    double targetFps = 60.0;
    double sampleRate = 44100.0;
    bool gameLoaded = false;

    std::mutex videoMutex;
    std::vector<uint8_t> rgbaFrame;
    unsigned frameWidth = 0;
    unsigned frameHeight = 0;
    bool frameUpdated = false;

    std::mutex audioMutex;
    std::vector<int16_t> audioRing;

    std::mutex inputMutex;
    uint32_t joypadMask[4]{0, 0, 0, 0};
    int16_t analogAxes[4][4]{{0}};

    std::vector<uint8_t> romMemoryBuffer;
};

LibretroInstance *g_active = nullptr;

bool cb_environment(unsigned cmd, void *data) {
    if (!g_active) return false;
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
            if (data) {
                *reinterpret_cast<const char **>(data) = g_active->systemDir.c_str();
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            if (data) {
                *reinterpret_cast<const char **>(data) = g_active->saveDir.c_str();
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            if (data) {
                g_active->pixelFormat = *reinterpret_cast<const int *>(data);
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE: {
            if (data) {
                *reinterpret_cast<bool *>(data) = true;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_OVERSCAN: {
            if (data) {
                *reinterpret_cast<bool *>(data) = false;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: {
            if (data) {
                *reinterpret_cast<bool *>(data) = false;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
            return true;
        default:
            return false;
    }
}

void cb_video_refresh(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!g_active || !data || width == 0 || height == 0) return;

    std::lock_guard<std::mutex> lock(g_active->videoMutex);
    g_active->frameWidth = width;
    g_active->frameHeight = height;
    g_active->rgbaFrame.resize(static_cast<size_t>(width) * height * 4);
    uint8_t *dst = g_active->rgbaFrame.data();
    const auto *srcBase = reinterpret_cast<const uint8_t *>(data);

    for (unsigned y = 0; y < height; ++y) {
        const uint8_t *row = srcBase + y * pitch;
        if (g_active->pixelFormat == RETRO_PIXEL_FORMAT_XRGB8888) {
            const auto *px = reinterpret_cast<const uint32_t *>(row);
            for (unsigned x = 0; x < width; ++x) {
                uint32_t c = px[x];
                dst[(y * width + x) * 4 + 0] = static_cast<uint8_t>((c >> 16) & 0xFF);
                dst[(y * width + x) * 4 + 1] = static_cast<uint8_t>((c >> 8) & 0xFF);
                dst[(y * width + x) * 4 + 2] = static_cast<uint8_t>(c & 0xFF);
                dst[(y * width + x) * 4 + 3] = 0xFF;
            }
        } else if (g_active->pixelFormat == RETRO_PIXEL_FORMAT_RGB565) {
            const auto *px = reinterpret_cast<const uint16_t *>(row);
            for (unsigned x = 0; x < width; ++x) {
                uint16_t c = px[x];
                uint8_t r = static_cast<uint8_t>(((c >> 11) & 0x1F) * 255 / 31);
                uint8_t g = static_cast<uint8_t>(((c >> 5) & 0x3F) * 255 / 63);
                uint8_t b = static_cast<uint8_t>((c & 0x1F) * 255 / 31);
                dst[(y * width + x) * 4 + 0] = r;
                dst[(y * width + x) * 4 + 1] = g;
                dst[(y * width + x) * 4 + 2] = b;
                dst[(y * width + x) * 4 + 3] = 0xFF;
            }
        } else {
            // 0RGB1555
            const auto *px = reinterpret_cast<const uint16_t *>(row);
            for (unsigned x = 0; x < width; ++x) {
                uint16_t c = px[x];
                uint8_t r = static_cast<uint8_t>(((c >> 10) & 0x1F) * 255 / 31);
                uint8_t g = static_cast<uint8_t>(((c >> 5) & 0x1F) * 255 / 31);
                uint8_t b = static_cast<uint8_t>((c & 0x1F) * 255 / 31);
                dst[(y * width + x) * 4 + 0] = r;
                dst[(y * width + x) * 4 + 1] = g;
                dst[(y * width + x) * 4 + 2] = b;
                dst[(y * width + x) * 4 + 3] = 0xFF;
            }
        }
    }
    g_active->frameUpdated = true;
}

void cb_audio_sample(int16_t left, int16_t right) {
    if (!g_active) return;
    std::lock_guard<std::mutex> lock(g_active->audioMutex);
    if (g_active->audioRing.size() < 32768) {
        g_active->audioRing.push_back(left);
        g_active->audioRing.push_back(right);
    }
}

size_t cb_audio_sample_batch(const int16_t *data, size_t frames) {
    if (!g_active || !data || frames == 0) return frames;
    std::lock_guard<std::mutex> lock(g_active->audioMutex);
    size_t maxAdd = std::min(frames * 2, size_t(32768) - std::min(g_active->audioRing.size(), size_t(32768)));
    if (maxAdd > 0) {
        g_active->audioRing.insert(g_active->audioRing.end(), data, data + maxAdd);
    }
    return frames;
}

void cb_input_poll(void) {
    // State is pushed asynchronously from ControllerInputManager
}

int16_t cb_input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (!g_active || port >= 4) return 0;
    std::lock_guard<std::mutex> lock(g_active->inputMutex);
    if (device == RETRO_DEVICE_JOYPAD) {
        if (id >= 32) return 0;
        return ((g_active->joypadMask[port] >> id) & 1u) ? 1 : 0;
    }
    if (device == RETRO_DEVICE_ANALOG && index <= 1 && id <= 1) {
        return g_active->analogAxes[port][index * 2 + id];
    }
    return 0;
}

template <typename Fn>
bool resolve(void *handle, const char *name, Fn &out) {
    out = reinterpret_cast<Fn>(dlsym(handle, name));
    if (!out) {
        LOGE("Missing symbol in core: %s", name);
        return false;
    }
    return true;
}

void saveSramIfPresent(LibretroInstance *inst) {
    if (!inst || !inst->gameLoaded || inst->sramPath.empty() || !inst->sym.retro_get_memory_data || !inst->sym.retro_get_memory_size) {
        return;
    }
    size_t sz = inst->sym.retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    void *ptr = inst->sym.retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (ptr && sz > 0) {
        std::ofstream out(inst->sramPath, std::ios::binary | std::ios::trunc);
        if (out.is_open()) {
            out.write(reinterpret_cast<const char *>(ptr), static_cast<std::streamsize>(sz));
        }
    }
}

void loadSramIfPresent(LibretroInstance *inst) {
    if (!inst || !inst->gameLoaded || inst->sramPath.empty() || !inst->sym.retro_get_memory_data || !inst->sym.retro_get_memory_size) {
        return;
    }
    size_t sz = inst->sym.retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    void *ptr = inst->sym.retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (ptr && sz > 0) {
        std::ifstream in(inst->sramPath, std::ios::binary);
        if (in.is_open()) {
            in.read(reinterpret_cast<char *>(ptr), static_cast<std::streamsize>(sz));
        }
    }
}

} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeIsLibraryAvailable(
    JNIEnv *env, jobject /*thiz*/, jstring jLibName
) {
    const char *libName = env->GetStringUTFChars(jLibName, nullptr);
    void *h = dlopen(libName, RTLD_LAZY);
    bool ok = (h != nullptr);
    if (h) dlclose(h);
    env->ReleaseStringUTFChars(jLibName, libName);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeInitialize(
    JNIEnv *env, jobject /*thiz*/, jstring jLibName, jstring jSystemDir, jstring jSaveDir
) {
    const char *libName = env->GetStringUTFChars(jLibName, nullptr);
    const char *sysDir = env->GetStringUTFChars(jSystemDir, nullptr);
    const char *savDir = env->GetStringUTFChars(jSaveDir, nullptr);

    auto *inst = new LibretroInstance();
    inst->systemDir = sysDir;
    inst->saveDir = savDir;
    inst->handle = dlopen(libName, RTLD_NOW);

    env->ReleaseStringUTFChars(jLibName, libName);
    env->ReleaseStringUTFChars(jSystemDir, sysDir);
    env->ReleaseStringUTFChars(jSaveDir, savDir);

    if (!inst->handle) {
        LOGE("dlopen failed: %s", dlerror());
        delete inst;
        return 0L;
    }

    bool ok = true;
    ok &= resolve(inst->handle, "retro_init", inst->sym.retro_init);
    ok &= resolve(inst->handle, "retro_deinit", inst->sym.retro_deinit);
    ok &= resolve(inst->handle, "retro_get_system_info", inst->sym.retro_get_system_info);
    ok &= resolve(inst->handle, "retro_get_system_av_info", inst->sym.retro_get_system_av_info);
    ok &= resolve(inst->handle, "retro_set_environment", inst->sym.retro_set_environment);
    ok &= resolve(inst->handle, "retro_set_video_refresh", inst->sym.retro_set_video_refresh);
    ok &= resolve(inst->handle, "retro_set_audio_sample", inst->sym.retro_set_audio_sample);
    ok &= resolve(inst->handle, "retro_set_audio_sample_batch", inst->sym.retro_set_audio_sample_batch);
    ok &= resolve(inst->handle, "retro_set_input_poll", inst->sym.retro_set_input_poll);
    ok &= resolve(inst->handle, "retro_set_input_state", inst->sym.retro_set_input_state);
    ok &= resolve(inst->handle, "retro_run", inst->sym.retro_run);
    ok &= resolve(inst->handle, "retro_load_game", inst->sym.retro_load_game);
    ok &= resolve(inst->handle, "retro_unload_game", inst->sym.retro_unload_game);

    inst->sym.retro_serialize_size = reinterpret_cast<size_t (*)()>(dlsym(inst->handle, "retro_serialize_size"));
    inst->sym.retro_serialize = reinterpret_cast<bool (*)(void *, size_t)>(dlsym(inst->handle, "retro_serialize"));
    inst->sym.retro_unserialize = reinterpret_cast<bool (*)(const void *, size_t)>(dlsym(inst->handle, "retro_unserialize"));
    inst->sym.retro_get_memory_data = reinterpret_cast<void *(*)(unsigned)>(dlsym(inst->handle, "retro_get_memory_data"));
    inst->sym.retro_get_memory_size = reinterpret_cast<size_t (*)(unsigned)>(dlsym(inst->handle, "retro_get_memory_size"));

    if (!ok) {
        dlclose(inst->handle);
        delete inst;
        return 0L;
    }

    g_active = inst;
    inst->sym.retro_set_environment(cb_environment);
    inst->sym.retro_set_video_refresh(cb_video_refresh);
    inst->sym.retro_set_audio_sample(cb_audio_sample);
    inst->sym.retro_set_audio_sample_batch(cb_audio_sample_batch);
    inst->sym.retro_set_input_poll(cb_input_poll);
    inst->sym.retro_set_input_state(cb_input_state);
    inst->sym.retro_init();

    return reinterpret_cast<jlong>(inst);
}

JNIEXPORT jboolean JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeLoadGame(
    JNIEnv *env, jobject /*thiz*/, jlong handlePtr, jstring jRomPath, jstring jSramPath
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (!inst) return JNI_FALSE;

    const char *romPath = env->GetStringUTFChars(jRomPath, nullptr);
    const char *sramPath = env->GetStringUTFChars(jSramPath, nullptr);
    inst->sramPath = sramPath;

    struct retro_system_info sysInfo{};
    inst->sym.retro_get_system_info(&sysInfo);

    struct retro_game_info gameInfo{};
    gameInfo.path = romPath;
    if (!sysInfo.need_fullpath) {
        std::ifstream file(romPath, std::ios::binary | std::ios::ate);
        if (file.is_open()) {
            std::streamsize size = file.tellg();
            file.seekg(0, std::ios::beg);
            if (size > 0 && size < 256L * 1024L * 1024L) {
                inst->romMemoryBuffer.resize(static_cast<size_t>(size));
                if (file.read(reinterpret_cast<char *>(inst->romMemoryBuffer.data()), size)) {
                    gameInfo.data = inst->romMemoryBuffer.data();
                    gameInfo.size = inst->romMemoryBuffer.size();
                }
            }
        }
    }

    g_active = inst;
    bool loaded = inst->sym.retro_load_game(&gameInfo);
    if (loaded) {
        inst->gameLoaded = true;
        struct retro_system_av_info avInfo{};
        inst->sym.retro_get_system_av_info(&avInfo);
        if (avInfo.timing.fps > 1.0) inst->targetFps = avInfo.timing.fps;
        if (avInfo.timing.sample_rate > 1000.0) inst->sampleRate = avInfo.timing.sample_rate;
        loadSramIfPresent(inst);
    }

    env->ReleaseStringUTFChars(jRomPath, romPath);
    env->ReleaseStringUTFChars(jSramPath, sramPath);
    return loaded ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jintArray JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeRunFrame(
    JNIEnv *env, jobject /*thiz*/, jlong handlePtr, jbyteArray jOutRgba
) {
    jint meta[3] = {0, 0, 0};
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (inst && inst->gameLoaded) {
        g_active = inst;
        inst->sym.retro_run();

        std::lock_guard<std::mutex> lock(inst->videoMutex);
        if (inst->frameUpdated && !inst->rgbaFrame.empty()) {
            jsize cap = env->GetArrayLength(jOutRgba);
            jsize copyLen = std::min(cap, static_cast<jsize>(inst->rgbaFrame.size()));
            env->SetByteArrayRegion(jOutRgba, 0, copyLen, reinterpret_cast<const jbyte *>(inst->rgbaFrame.data()));
            meta[0] = static_cast<jint>(inst->frameWidth);
            meta[1] = static_cast<jint>(inst->frameHeight);
            meta[2] = 1;
            inst->frameUpdated = false;
        }
    }
    jintArray arr = env->NewIntArray(3);
    env->SetIntArrayRegion(arr, 0, 3, meta);
    return arr;
}

JNIEXPORT jint JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativePollAudio(
    JNIEnv *env, jobject /*thiz*/, jlong handlePtr, jshortArray jOutSamples
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (!inst) return 0;
    std::lock_guard<std::mutex> lock(inst->audioMutex);
    if (inst->audioRing.empty()) return 0;

    jsize cap = env->GetArrayLength(jOutSamples);
    jsize count = std::min(cap, static_cast<jsize>(inst->audioRing.size()));
    env->SetShortArrayRegion(jOutSamples, 0, count, inst->audioRing.data());
    inst->audioRing.erase(inst->audioRing.begin(), inst->audioRing.begin() + count);
    return count;
}

JNIEXPORT void JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeSetInput(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong handlePtr, jint port, jint joypadMask,
    jint lx, jint ly, jint rx, jint ry
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (!inst || port < 0 || port >= 4) return;
    std::lock_guard<std::mutex> lock(inst->inputMutex);
    inst->joypadMask[port] = static_cast<uint32_t>(joypadMask);
    inst->analogAxes[port][0] = static_cast<int16_t>(lx);
    inst->analogAxes[port][1] = static_cast<int16_t>(ly);
    inst->analogAxes[port][2] = static_cast<int16_t>(rx);
    inst->analogAxes[port][3] = static_cast<int16_t>(ry);
}

JNIEXPORT jboolean JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeSaveState(
    JNIEnv *env, jobject /*thiz*/, jlong handlePtr, jstring jPath
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (!inst || !inst->gameLoaded || !inst->sym.retro_serialize_size || !inst->sym.retro_serialize) {
        return JNI_FALSE;
    }
    size_t sz = inst->sym.retro_serialize_size();
    if (sz == 0) return JNI_FALSE;
    std::vector<uint8_t> buf(sz);
    if (!inst->sym.retro_serialize(buf.data(), sz)) return JNI_FALSE;

    const char *path = env->GetStringUTFChars(jPath, nullptr);
    std::ofstream out(path, std::ios::binary | std::ios::trunc);
    bool ok = out.is_open();
    if (ok) out.write(reinterpret_cast<const char *>(buf.data()), static_cast<std::streamsize>(sz));
    env->ReleaseStringUTFChars(jPath, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeLoadState(
    JNIEnv *env, jobject /*thiz*/, jlong handlePtr, jstring jPath
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (!inst || !inst->gameLoaded || !inst->sym.retro_unserialize) {
        return JNI_FALSE;
    }
    const char *path = env->GetStringUTFChars(jPath, nullptr);
    std::ifstream in(path, std::ios::binary | std::ios::ate);
    if (!in.is_open()) {
        env->ReleaseStringUTFChars(jPath, path);
        return JNI_FALSE;
    }
    std::streamsize sz = in.tellg();
    in.seekg(0, std::ios::beg);
    std::vector<uint8_t> buf(static_cast<size_t>(sz));
    bool readOk = static_cast<bool>(in.read(reinterpret_cast<char *>(buf.data()), sz));
    env->ReleaseStringUTFChars(jPath, path);
    if (!readOk) return JNI_FALSE;
    return inst->sym.retro_unserialize(buf.data(), buf.size()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdouble JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeGetSampleRate(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong handlePtr
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    return inst ? inst->sampleRate : 44100.0;
}

JNIEXPORT void JNICALL
Java_com_cyrus_emulator_nativebridge_NativeLibretroBridge_nativeDispose(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong handlePtr
) {
    auto *inst = reinterpret_cast<LibretroInstance *>(handlePtr);
    if (!inst) return;
    if (inst->gameLoaded) {
        saveSramIfPresent(inst);
        inst->sym.retro_unload_game();
        inst->gameLoaded = false;
    }
    inst->sym.retro_deinit();
    if (inst->handle) dlclose(inst->handle);
    if (g_active == inst) g_active = nullptr;
    delete inst;
}

} // extern "C"
