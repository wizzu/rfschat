RFSChat - Really Fine and Simple Chat
=====================================

RFSChat is a simple multiuser chat application for demo purposes.


Building
--------

The program can be compiled using the compile.sh shell script.


Running
-------

The program can be started using the run.sh shell script.

Manually running "java -cp <classdir>/ChatServer <listenport>" is also possible.


Using
-----

After the program has been started, clients can connect to the server on the
port specified during server startup. E.g. "telnet <server-hostname> 1234".

Clients can send simple one-line text messages to the server.

The following case-sensitive commands available:
* HELP - displays a help text
* NICK - change the client's nickname
* WHO - list connected users
* QUIT - disconnect from the chat service

Messages not recognised as valid commands are passed on to other connected
users.
