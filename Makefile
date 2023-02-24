BASE_PATH := ./app/src/main/java
PKG_PATH := edu/riccardomori/wordle
PROJECT_PATH := $(BASE_PATH)/$(PKG_PATH)
BUILD_PATH := ./build
BUILD_FULL_PATH := $(BUILD_PATH)/$(PKG_PATH)

SRC := server/ServerMain.class server/WordleServer.class server/WordleServerCore.class \
	client/ClientMain.class \
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
	sed -i 's/server.ServerMain/client.ClientMain/g' $(BUILD_PATH)/META-INF/MANIFEST.MF
	cd $(BUILD_PATH) && jar -c -v -m META-INF/MANIFEST.MF -f client.jar $$(find . -name '*.class')
	sed -i 's/client.ClientMain/server.ServerMain/g' $(BUILD_PATH)/META-INF/MANIFEST.MF
	cd $(BUILD_PATH) && jar -c -v -m META-INF/MANIFEST.MF -f server.jar $$(find . -name '*.class')
	mv $(BUILD_PATH)/client.jar $(BUILD_PATH)/server.jar ./

all: $(SRC_FULL)

gradle-jar:
	./gradlew build
	./gradlew clientJar
	./gradlew serverJar
	cp ./app/build/libs/client.jar ./gradle-client.jar
	cp ./app/build/libs/server.jar ./gradle-server.jar

clean:
	rm -f wordle.jar
	rm -rf $(BUILD_PATH)
