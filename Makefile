SRC := src
BUILD ?= build
RES := res
ASSETS := assets
ANDROID ?= /opt/android-tools/sdk/platforms/android-21/android.jar
KEYS ?= $(HOME)/.android/$(USER).keystore

BUILD_TOOLS ?= /opt/android-tools/sdk/build-tools/25.0.0
DX := $(BUILD_TOOLS)/dx
AAPT := $(BUILD_TOOLS)/aapt
SIGNER := $(BUILD_TOOLS)/apksigner
ALIGNER := $(BUILD_TOOLS)/zipalign

JAVAC := javac
JAVAC_FLAGS := -d $(BUILD) -source 1.7 -target 1.7 -sourcepath $(SRC) -classpath $(BUILD) -bootclasspath $(ANDROID)
APP := ajournal
PACKAGE := newpson/$(APP)

SOURCES := $(wildcard $(SRC)/$(PACKAGE)/*.java)
OBJECTS := $(subst $(SRC),$(BUILD),$(patsubst %.java,%.class,$(SOURCES)))

$(BUILD)/$(PACKAGE)/%.class: $(SRC)/$(PACKAGE)/%.java
	$(JAVAC) $(JAVAC_FLAGS) $<

all: check $(APP).apk

$(APP).apk: $(APP)-unaligned.apk
	$(ALIGNER) -f 4 $< $@
	$(SIGNER) sign --ks $(KEYS) $@

$(APP)-unaligned.apk: classes.dex
	$(AAPT) package -f -F $@ -M AndroidManifest.xml -S $(RES) -A $(ASSETS) -I $(ANDROID)
	$(AAPT) add $@ $<

classes.dex: $(BUILD)/$(PACKAGE)/R.class $(OBJECTS)
	$(DX) --dex --output=$@ $(BUILD)

$(SRC)/$(PACKAGE)/R.java: AndroidManifest.xml
	$(AAPT) package -f -m -J $(SRC) -S $(RES) -M $< -I $(ANDROID)

.PHONY: all clean check
check:
	@if ! [ -d "build" ]; then mkdir "build"; fi

clean:
	rm -f $(SRC)/$(PACKAGE)/R.java
	rm -rf $(BUILD)/*
	rm -f classes.dex $(APP)*.apk

