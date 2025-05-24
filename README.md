# CPD Projects

CPD Projects of group T03G11.

Group members:

1. Beatriz Bernardo (up202206097@up.pt)
2. Beatriz Sonnemberg (up202206098@up.pt)
3. Daniel Silva (up201909935@up.pt)

## Distributed Systems Assignment

### How to Compile

- `javac -d out *.java`

### How to Run

- `java -cp out ChatServer.java [<port>]`
- `java -cp out ClientConsoleUI [<host>] [<port>]`  
**Example:**
- `java -cp out ChatServer.java 1234`
- `java -cp out ClientConsoleUI localhost 1234`  

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