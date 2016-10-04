.PHONY: clean default all
	
MVN_CFG = pom.xml
SRC_FILES := $(shell find src -type f)

default: all
	
all: stine_calendar_bot.jar
	
stine_calendar_bot.jar: $(MVN_CFG) $(SRC_FILES)
	mvn package shade:shade
	cp target/stine_calendar_bot.jar stine_calendar_bot.jar
	chmod +x stine_calendar_bot.jar
	
clean:
	mvn clean
	rm -f stine_calendar_bot.jar dependency-reduced-pom.xml 2>/dev/null
