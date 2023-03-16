PRJ_PATH := ./app/src/main
BASE_PATH := $(PRJ_PATH)/java
PKG_PATH := edu/riccardomori/wordle
PROJECT_PATH := $(BASE_PATH)/$(PKG_PATH)
BUILD_PATH := ./build
BUILD_FULL_PATH := $(BUILD_PATH)/$(PKG_PATH)
RESOURCES_PATH := $(PRJ_PATH)/resources

SRC := $(shell find $$BASE_PATH -name '*.java' | while read aa; do echo $${aa#*app/src/main/java/edu/riccardomori/wordle/}; done)
SRC := $(SRC:.java=.class)
SRC_FULL := $(addprefix $(BUILD_FULL_PATH)/,$(SRC))

JC := javac
JFLAGS := -cp lib/gson-2.10.1.jar:$(BASE_PATH): -d $(BUILD_PATH)

.PHONY: clean all jar gradle-jar

default: all


$(BUILD_PATH)/$(PKG_PATH)/%.class: $(PROJECT_PATH)/%.java
	$(JC) $(JFLAGS) $<

jar: all
	cp -r META-INF $(BUILD_PATH)/
	cp -r $(RESOURCES_PATH)/* $(BUILD_PATH)
	sed -i 's/server.ServerMain/client.ClientMain/g' $(BUILD_PATH)/META-INF/MANIFEST.MF
	cd $(BUILD_PATH) && jar -c -v -m META-INF/MANIFEST.MF -f client.jar $$(find . -name '*.class') $$(find .)
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
