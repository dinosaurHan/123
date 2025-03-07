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
│   ├──SessionService.java
│   ├──StakeService.java
```
## Getting Started
### Prerequisites
- JDK 1.8+
- Maven 3.6+
### Build
```text
mvn clean package
```
### Run
```text
java -jar target/Betting-server-1.0.jar
```
### Verify
Test basic functionality:
```text
curl http://localhost:8001/1234/session
```
Expected response 
```text
httpstatuscode ： 200
httpresponsebody ：sessionKey (VF3VQOU etc.)
```
### API Documentation
| Endpoint               | Method | Parameters               | Status Codes           |
|------------------------|--------|--------------------------|------------------------|
| `/{customerId}/sessions/`       | GET    | `id: int`                | 200 OK,response body :`session key`, 400 Bad Request|
| `/{betId}/stake?sessionkey={sessinoKey}`      | POST   | `betid:int ；sessionkey: string ；body: stake amount:int`     | 200 OK, 401 Unauthorized|
| `/{betId}/highstakes`      | POST/GET   |      | 200 OK ,response body :`stake list 9002=1500,9001=800`|

#### Example Requests
- Create session
curl -X GET [http://localhost:8001/666/session](http://localhost:8001/666/session)

- Submit stake (amount=5000)
curl -X POST -d "5000" [http://localhost:8001/1234/stake?sessionkey=abc123](http://localhost:8001/1234/stake?sessionkey=abc123)
