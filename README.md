# Chat-Server
A chat system that allows for user to broadcast public messages and supports private communication between users 

The Chat code waits for incoming client connections on port 69, assigns a coordinator, and handles message broadcasting while aintains real-time communication 
It also supports private messages and sends detailed member information (including IP and port) to clients.

The client code asks users for server details and client ID, ensuring a smooth connection to the server and manages user inputs for normal and private messages, and handles disconnections
