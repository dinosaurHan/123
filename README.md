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