BASE_PATH := ./app/src/main/java
PKG_PATH := edu/riccardomori/wordle
PROJECT_PATH := $(BASE_PATH)/$(PKG_PATH)
BUILD_PATH := ./build
BUILD_FULL_PATH := $(BUILD_PATH)/$(PKG_PATH)

SRC := server/ServerMain.class server/WordleServer.class server/WordleServerCore.class \
	server/WordleServerRMI.class logging/ConsoleHandler.class Action.class \
	ClientState.class
SRC_FULL := $(addprefix $(BUILD_FULL_PATH)/,$(SRC))

JC := javac
JFLAGS := -cp lib/gson-2.10.1.jar:$(BASE_PATH): -d $(BUILD_PATH)

.PHONY: clean all jar gradle-jar

default: all


$(BUILD_PATH)/$(PKG_PATH)/%.class: $(PROJECT_PATH)/%.java
	$(JC) $(JFLAGS) $<

jar: all
	cp -r META-INF $(BUILD_PATH)/
	cd $(BUILD_PATH) && jar -c -v -m META-INF/MANIFEST.MF -f wordle.jar $$(find . -name '*.class')
	mv $(BUILD_PATH)/wordle.jar ./wordle.jar

all: $(SRC_FULL)

gradle-jar:
	./gradlew build
	cp ./app/build/libs/wordle.jar ./wordle-gradle.jar

clean:
	rm -f wordle.jar
	rm -rf $(BUILD_PATH)
