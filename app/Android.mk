LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := app
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \

LOCAL_C_INCLUDES += C:\Users\tpz\AndroidStudioProjects\CardRecog\app\src\main\jni
LOCAL_C_INCLUDES += C:\Users\tpz\AndroidStudioProjects\CardRecog\app\src\main\jniLibs
LOCAL_C_INCLUDES += C:\Users\tpz\AndroidStudioProjects\CardRecog\app\src\debug\jni

include $(BUILD_SHARED_LIBRARY)
