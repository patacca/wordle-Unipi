JC := javac
JFLAGS :=

PROJECT_PATH := ./edu/riccardomori/wordle

.PHONY: clean all jar

default: all


$(PROJECT_PATH)/%.class: $(PROJECT_PATH)/%.java
	$(JC) $(JFLAGS) $<

jar: all
	jar -c -v -m META-INF/MANIFEST.MF -f wordle.jar $(PROJECT_PATH)/*.class

all: edu/riccardomori/wordle/ServerMain.class

clean:
	rm -f wordle.jar
	cd $(PROJECT_PATH) && rm -f *.class
