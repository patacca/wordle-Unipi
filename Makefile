JC := javac
JFLAGS :=

PROJECT_PATH := ./edu/riccardomori/wordle

.PHONY: clean all jar

default: all


$(PROJECT_PATH)/%.class: $(PROJECT_PATH)/%.java
	$(JC) $(JFLAGS) $<

jar: all
	jar -c -v -m META-INF/MANIFEST.MF -f wordle.jar $$(find $(PROJECT_PATH) -name '*.class')

all: $(PROJECT_PATH)/ServerMain.class $(PROJECT_PATH)/WordleServer.class \
	$(PROJECT_PATH)/logging/ConsoleHandler.class

clean:
	rm -f wordle.jar
	find $(PROJECT_PATH) -name '*.class' -exec rm -f '{}' \;
