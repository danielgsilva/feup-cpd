# CPD Projects

CPD Projects of group T03G11:
- [Assignment 1](#parallel-computing-assignment)
- [Assignment 2](#distributed-systems-assignment)

Group members:

1. Beatriz Bernardo 
2. Beatriz Sonnemberg 
3. Daniel Silva 

## [Parallel Computing Assignment](./assign1/doc/CPD%20Relatório%20-%20Projeto%201.pdf)
#### Grade: 18.5/20

### Analysis of Matrix Multiplication

This project had the main objective of analyzing the impact of memory hierarchy and parallelization approach on the performance of large-volume matrix multiplication operations. We used the Performance API (PAPI) to collect crucial performance metrics, such as execution time, and the number of L1 and L2 cache misses.

#### Single-Core Implementations

Three approaches to matrix multiplication were implemented and compared in single-core, focusing on how data access in memory affects cache utilization:

- `Simple Matrix Multiplication`: The basic approach, but inefficient in terms of cache. Due to accessing the columns of the second matrix, jumps occur between distant memory positions, resulting in a high number of cache misses.

- `Line Matrix Multiplication`: Multiplies each element of a row in the first matrix by the entire corresponding row in the second matrix, accumulating values in the result matrix. This method allows sequential memory access, leveraging spatial locality of the cache, and is significantly more efficient than the simple algorithm.

- `Block Matrix Multiplication`: Divides the matrices into smaller blocks for multiplication. It proved to have the best performance as it takes better advantage of spatial locality, as the smaller data blocks are more likely to fit in the cache.

#### Multi-Core Implementations

For the multi-core environment, two parallel versions of the `Line Matrix Multiplication` algorithm were analyzed using `OpenMP`. The focus was on managing concurrency (race conditions) and synchronization between threads.

- `Outer Loop Parallelized`(OMP Version 1): Parallelizes the outermost loop. It demonstrated significantly better performance than the second version.

- `Inner Loop Parallelized`(OMP Version 2): Parallelizes the inner loop. This version resulted in inferior performance due to the overhead of the synchronization enforced by the `#pragma omp for` directive on the inner loop , implying duplicated work  and wasted processing power compared to OMP V1.

The analysis highlighted that the choice of algorithm and how it manages memory access and thread concurrency are the main factors influencing efficiency, demonstrating the importance of good memory access management and reducing synchronization overhead in parallel solutions.


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


