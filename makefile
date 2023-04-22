default: clean
	clear
	javac ConnectionHandler.java Server.java
	java Server
	
clean:
	rm -rf *.class

controller:
	clear
	javac Controller.java
	java Controller