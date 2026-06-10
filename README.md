<p align="center">
  <img src="./Logo.png" alt="B.O.B.D.S. Logo" width="150" />
</p>

<h1 align="center">B.O.B.D.S. Hardware Simulator</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java" />
</p>

## About The Project

B.O.B.D.S. Hardware Simulator is a standalone Java application designed to mock the physical behavior of B.O.B.D.S. robotic units. It listens for incoming operational commands from the main server, queues them, processes them over simulated timeframes, and continuously streams status updates (Unit Status and Order Status) back to the backend.

## Key Features

* **HTTP Server:** Implements a lightweight `com.sun.net.httpserver.HttpServer` listening on port `7777` to receive external payloads.
* **Asynchronous Queueing:** Uses concurrent threads to simulate execution times for received orders.
* **State Syncing:** Automatically updates the backend REST API whenever a unit wakes up, finishes an order, or goes idle.
* **Logging & Tracking:** Verbose console output tracking order lifecycle (Received > Queued > In Progress > Finished).

## Prerequisites

* Java Development Kit (JDK 17 or higher recommended)

## Installation & Configuration

1. Clone the repository.
2. Navigate to the `BODS_2.0_Simulator/src` directory.
3. The simulator requires the backend server to be running on `http://localhost:8081` to sync statuses successfully.

> [!IMPORTANT]
> Always start the B.O.B.D.S. Server before launching the Simulator to ensure successful real-time state synchronization via HTTP POST requests.

## Running Locally

To run the simulator and connect it to the server:

1. Compile the source files:
   ```bash
   javac com/bobds/simulador/Main.java com/bobds/simulador/RobotService.java
   ```
2. Execute the compiled Main class:
   ```bash
   java com.bobds.simulador.Main
   ```
3. The simulator will output: `Simulador corriendo en puerto 7777...`

> [!NOTE]
> When the backend sends a command, the simulator will automatically intercept it, process it, and make `POST` requests back to the server to update the state of both the Unit and the Order in real-time.

> [!CAUTION]
> The simulator listens on port `7777`. Ensure no other application on your machine is using this port, otherwise the `HttpServer` will fail to bind and crash on startup.

## Architecture

* `Main.java`: Starts the HTTP server on port 7777 and exposes the `/orden` context to receive JSON payloads containing `idUnidad`, `idOrden`, and `orden`.
* `RobotService.java`: Manages the threading logic to delay execution randomly (simulating physical tasks) and sends state updates (`ACTIVA`/`INACTIVA`, `EN CURSO`/`FINALIZADA`) back to the B.O.B.D.S. Server.