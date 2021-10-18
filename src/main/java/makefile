JC = javac
JR = java
JFLAGS  = -g -Wall

default: compile

compile:
	mkdir out
	$(JC) -d out/ ./src/*.java

clean:
	$(RM) -r out

run:
	$(JR) -classpath out Main