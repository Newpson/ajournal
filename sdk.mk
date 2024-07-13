SDK_ROOT ?= /opt/android/sdk
PLATFORM_VERSION ?= android-28
BUILD_TOOLS_VERSION ?= 35.0.0-rc4
KEYS ?= $(HOME)/.android/$(USER).keystore

ANDROID := $(SDK_ROOT)/platforms/$(PLATFORM_VERSION)/android.jar
BUILD_TOOLS := $(SDK_ROOT)/build-tools/$(BUILD_TOOLS_VERSION)
JAVAC := javac
