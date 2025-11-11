# CPD Projects

CPD Projects of group T03G11.

Group members:

1. Beatriz Bernardo 
2. Beatriz Sonnemberg 
3. Daniel Silva 

## [Distributed Systems Assignment](./assign2/)
#### Grade: 18.25/20

This project implements a concurrent client-server chat system in Java using TCP sockets. The system allows authenticated users to exchange text messages in different chat rooms, including special AI-powered rooms that integrate with a local LLM (Ollama).

### Ollama

- To start virtual machine 
    - `sudo docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama14 ollama/ollama`

- To test
    - `sudo docker exec -it ollama14 ollama run llama3`

### How to Compile

- `javac -d out *.java`

### How to Run

- `java -cp out ChatServer.java [<port>]`
- `java -cp out ClientConsoleUI [<host>] [<port>]`  
**Example:**
- `java -cp out ChatServer.java 1234`
- `java -cp out ClientConsoleUI localhost 1234`  

### User Credentials

- **Users**: biab, bias, daniel
- **Password**: 1234

### Debug Server Message

- `ChatClient.java`: Uncomment line 391 - method *handleServerMessage()*

### Protocol

- **Authentication**
    - CLIENT: `REGISTER <username> <password>`
    - SERVER: `REGISTER_SUCCESS` or `REGISTER_FAILURE`
    <br><br>  
    - CLIENT: `LOGIN <username> <password>`
    - SERVER: `LOGIN_SUCCESS <token>` or `LOGIN_FAILURE`
    <br><br>  
    - CLIENT: `RECONNECT <token>`
    - SERVER: `RECONNECT_SUCCESS` or `RECONNECT_FAILURE`
    <br><br>  
    - CLIENT: `LOGOUT`
    - SERVER: `LOGOUT_SUCCESS`

- **Room Management**
    - CLIENT: `LIST_ROOMS`
    - SERVER: `ROOMS <room1> <room2> <room3> ...`
    <br><br>
    - CLIENT: `CREATE_ROOM <roomName>`
    - SERVER: `ROOM_CREATED <roomName>` or `ROOM_EXISTS <roomName>`
    <br><br>
    - CLIENT: `CREATE_AI_ROOM <roomName> <prompt>`
    - SERVER: `ROOM_CREATED <roomName>` or `ROOM_EXISTS <roomName>`
    <br><br>
    - CLIENT: `JOIN_ROOM <roomName>`
    - SERVER: `JOINED <roomName>` or `ROOM_NOT_FOUND <roomName>`
    - SERVER: `USER_JOINED <username>` (broadcast to all users in room)
    <br><br>
    - CLIENT: `LEAVE_ROOM`
    - SERVER: `LEFT_ROOM <roomName>`
    - SERVER: `USER_LEFT <username>` (broadcast to all users in room)

- **Messaging**
    - CLIENT: `MESSAGE <roomName> <messageContent>`
    - SERVER: `MESSAGE <roomName> <username> <messageContent>` (broadcast to all users in room)


