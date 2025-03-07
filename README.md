# Betting Server

## Project Overview
A high-concurrency betting platform server built with Java native HTTP Server, providing stake recording, session management, and real-time leaderboard features.

**Technology Stack**
- JDK Version: **1.8+**
- Web Framework: `com.sun.net.httpserver` (JDK built-in)
- Concurrency Model: `ConcurrentHashMap`, `ThreadPoolExecutor`
- Data Structures: `ConcurrentSkipListMap`
- Build Tool: Maven

## Project Structure
```text
src/main/java/com/betbrain/
├── handler/               # Request Handlers
│   ├── HighStakesHandler.java
│   ├── SessionHandler.java
│   ├── StakeHandler.java
│   └── ServiceUnavailableRejectionHandler.java
├── model/                 # Data Models
│   ├── BetEvent.java
│   └── Session.java
├── server/                # Server Core
│   ├── Handler.java
│   └── Router.java
└── service/               # Business Services
├── SessionService.java
└── StakeService.java

##Getting Started
###Prerequisites
-JDK 11+
-Maven 3.6+
Build
mvn clean package
Run
java -jar target/betting-server.jar
Verify
Test basic functionality:

curl http://localhost:8080/sessions/1001
API Documentation
Endpoints
Endpoint	Method	Parameters	Success Response
/sessions/{customerId}	GET	-	200 OK (session key)
/stakes/{betId}	POST	sessionkey, stake value	200 OK
/highstakes/{betId}	GET	-	200 OK (CSV data)
Example Requests
# Create session
curl -X GET http://localhost:8080/sessions/1001

# Submit stake (amount=5000)
curl -X POST -d "5000" http://localhost:8080/stakes/2001?sessionkey=abc123

# Get top stakes
curl http://localhost:8080/highstakes/2001
Response Examples
Success (Stake Submission):

HTTP/1.1 200 OK
Error (Invalid Session):


HTTP/1.1 401 Unauthorized
Authentication failed