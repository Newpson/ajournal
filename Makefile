include sdk.mk

SRC := src
BUILD ?= build
RES := res
ASSETS := assets

D8 := $(BUILD_TOOLS)/d8
AAPT := $(BUILD_TOOLS)/aapt
SIGNER := $(BUILD_TOOLS)/apksigner
ALIGNER := $(BUILD_TOOLS)/zipalign

JAVAC_FLAGS := -d $(BUILD) -classpath $(ANDROID) -sourcepath $(SRC)
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
	$(D8) --lib $(ANDROID) $(BUILD)/$(PACKAGE)/*.class

$(SRC)/$(PACKAGE)/R.java: AndroidManifest.xml
	$(AAPT) package -f -m -J $(SRC) -S $(RES) -M $< -I $(ANDROID)

.PHONY: all clean check
check:
	@if ! [ -d "build" ]; then mkdir "build"; fi

clean:
	rm -f $(SRC)/$(PACKAGE)/R.java
	rm -rf $(BUILD)/*
	rm -f *.dex $(APP)*.apk*
