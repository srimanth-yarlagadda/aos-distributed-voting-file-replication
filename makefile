default: clean
	clear
	javac ConnectionHandler.java Server.java
	java Server
	
clean:
	rm -rf Server\$*.class
	rm -rf Server.class