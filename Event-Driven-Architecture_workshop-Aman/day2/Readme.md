# Event-Driven Microservices with Kafka & Spring Boot - Aman Jha

This project is a hands-on demonstration of an Event-Driven Architecture (EDA) using Apache Kafka as the central messaging backbone. It showcases how to build a decoupled, scalable system with multiple microservices that communicate asynchronously.

## 1. Architecture Overview

The system consists of three independent microservices that are completely decoupled from one another. They communicate only through a central Kafka topic, `user-events`.

*   **Producer (`user-service`)**: Simulates a service that creates user events (e.g., a user login). It publishes these events to the Kafka topic and has no knowledge of who, if anyone, is listening.
*   **Kafka**: Acts as the durable, high-throughput message bus. It stores the events and makes them available for consumption.
*   **Consumers (`audit-service` & `notification-service`)**: These services subscribe to the `user-events` topic to receive a copy of every message. They run in different consumer groups, ensuring they both process all events independently for their own business purposes.

### Data Flow Diagram

```
+----------------+      (1) Publishes     +----------------+      (2) Consumes      +----------------+
|                |      UserEvent         |                |      UserEvent         |                |
|  User Service  | ---------------------> |     Kafka      | ---------------------> |  Audit Service |
|   (Producer)   |                        | (user-events)  |                        |  (Consumer A)  |
|                |                        |     Topic      |                        |                |
+----------------+                        +----------------+                        +----------------+
                                                 |
                                                 | (3) Consumes
                                                 | UserEvent
                                                 v
                                          +---------------------+
                                          |                     |
                                          | Notification Service|
                                          |    (Consumer B)     |
                                          |                     |
                                          +---------------------+
```

## 2. Project Modules

*   **`event-model-lib`**: A shared library (plain JAR) containing the common data model (`UserEvent.java`). This acts as the data contract between all services.
*   **`user-service`**: A Spring Boot application that acts as the event producer.
*   **`audit-service`**: A Spring Boot application that acts as a consumer, logging events for auditing purposes.
*   **`notification-service`**: A second Spring Boot application that acts as a consumer, simulating sending notifications based on events.

## 3. Technology Stack

*   Java 21
*   Spring Boot 3.1.5
*   Apache Kafka 3.5+
*   Maven
*   Docker & Docker Compose

## 4. Prerequisites

*   JDK 21 or newer
*   Apache Maven
*   Docker Desktop

## 5. Setup & Run Instructions

Follow these steps to get the entire system running.

### Step 1: Start the Kafka Environment

This project uses a simple, KRaft-based Kafka setup managed by Docker Compose.

1.  Open a terminal in the project root directory (`day2/`).
2.  Run the following command to start the Kafka container in the background:

    ```sh
    docker compose up -d
    ```

3.  Verify that the container is running and healthy:

    ```sh
    docker compose ps
    ```
    You should see the `kafka` container with a status of `Up`.

*(The final, working `docker-compose.yml` is included in the root of this project.)*

### Step 2: Build the Project

Build all the microservice modules using Maven.

1.  In the same terminal, run the Maven build command:

    ```sh
    mvn clean install
    ```
    This will compile all the code and create executable `.jar` files in the `target` directory of each service module.

### Step 3: Run the Microservices

You will need **three separate terminals** to run each service simultaneously.

*   **Terminal 1 - Start Audit Service (Consumer A):**
    ```sh
    java -jar audit-service/target/audit-service-1.0-SNAPSHOT.jar
    ```

*   **Terminal 2 - Start Notification Service (Consumer B):**
    ```sh
    java -jar notification-service/target/notification-service-1.0-SNAPSHOT.jar
    ```

*   **Terminal 3 - Start User Service (Producer):**
    ```sh
    java -jar user-service/target/user-service-1.0-SNAPSHOT.jar
    ```

### Step 4: Verify the Output

Once the `user-service` starts, it will begin sending messages. You will see logs appear in all three terminals, demonstrating the end-to-end flow.

*   **The User Service terminal** will show logs for sending events.
*   **The Audit Service terminal** will show logs for receiving events and "auditing" them.
*   **The Notification Service terminal** will show logs for receiving the same events and "sending notifications".

## 6. Screenshots of a Successful Run

### User Service (Producer) Output

![User Service Logs](screenshots/img_2.png)
*Caption: The `user-service` log shows it is sending `UserEvent` messages with randomly generated numeric `user_id` values.*

### Audit Service (Consumer) Output

![Audit Service Logs](screenshots/img.png)
*Caption: The `audit-service` log shows it is receiving the events and processing them for its own business logic (auditing).*

### Notification Service (Consumer) Output

![Notification Service Logs](screenshots/img_1.png)
*Caption: The `notification-service` log confirms it is also receiving a copy of every event, completely independent of the audit service.*

---

## 7. Core Kafka Concepts: Q&A

### What is the publish-subscribe model in Kafka?

**Answer:**
The publish-subscribe (pub-sub) model is a messaging pattern where producers publish messages to a "topic" without any knowledge of the downstream consumers. Consumers, in turn, can subscribe to topics they are interested in to receive messages. This model decouples the message sender from the receiver, allowing for asynchronous and highly scalable communication. A single message published to a topic can be broadcast to and processed independently by many different consumers.

**Use Case Example:**
Imagine an e-commerce platform launches a new product. A `product-service` publishes a single `NewProductAdded` event to a "products" topic.
*   The `inventory-service` consumes this event to update stock levels.
*   The `marketing-service` consumes it to trigger a promotional email campaign.
*   The `recommendation-service` consumes it to update its machine learning models.
    The `product-service` doesn't know or care about these other services; it just announces the event.

### How does Kafka ensure message durability?

**Answer:**
Kafka ensures that once a message is successfully written, it will not be lost, even if a server fails. It achieves this through a multi-layered strategy:
1.  **Replication:** Topic partitions are replicated across multiple servers (brokers). If one server fails, a replica on another server can take over.
2.  **Filesystem Persistence:** All messages are written to the disk's filesystem cache, making them durable across server restarts.
3.  **Producer Acknowledgments (`acks`):** Producers can be configured to wait for confirmation that a message has been safely replicated to a configurable number of brokers (`acks=all`) before considering the write successful. This provides the strongest guarantee against data loss.

**Use Case Example:**
A financial services application processes payment transactions. When a user makes a payment, a `payment-service` publishes a `PaymentReceived` event. This event **must not be lost**. The producer is configured with `acks=all` and the topic has a replication factor of 3. This guarantees that the payment event is safely written to three different servers before the user is shown a "Payment Successful" confirmation, eliminating the risk of data loss.

### What is the role of a Kafka topic and partition?

**Answer:**
*   **Topic:** A topic is a logical name for a category or stream of messages. Producers write messages to topics, and consumers read from them. It's the primary way to organize and identify data streams in Kafka.
*   **Partition:** A topic is split into one or more partitions. A partition is an ordered, immutable sequence of messages. Partitions are the fundamental unit of parallelism and scalability in Kafka. Splitting a topic into multiple partitions allows the data to be distributed across multiple brokers and consumed in parallel by multiple consumers from the same group. Kafka only guarantees message ordering *within* a single partition.

**Use Case Example:**
A ride-sharing app tracks the real-time location of its drivers. All location updates are sent to a `driver-locations` topic.
*   **Topic:** `driver-locations` organizes all location data.
*   **Partitions:** The topic is partitioned by `driver_id`. This ensures that all location updates for the *same driver* always go to the *same partition*. A consumer processing this partition will receive the locations for that driver in the exact order they occurred, which is critical for tracking a trip. Meanwhile, location data for thousands of other drivers can be processed in parallel on other partitions.

### What happens if a consumer fails while processing a message?

**Answer:**
When a consumer that is part of a consumer group fails, Kafka's fault tolerance mechanisms are triggered. The Kafka broker detects the failure (usually via a missed heartbeat) and initiates a "rebalance" of the consumer group. During the rebalance, the partitions that were assigned to the failed consumer are automatically reassigned to the remaining healthy consumers in the group. The new consumers will pick up processing from the last "committed offset" of the failed consumer, ensuring that all messages are eventually processed. This provides an "at-least-once" delivery guarantee by default.

**Use Case Example:**
An order processing system has a `new-orders` topic and a consumer group of three application instances processing these orders. If one of the instances crashes due to a server failure, Kafka detects this and reassigns its partitions to the other two instances. Those instances will start processing the orders that the failed consumer was responsible for, ensuring that no customer orders are dropped or ignored.

### Compare Kafka with another messaging system like RabbitMQ.

**Answer:**
Kafka and RabbitMQ are both powerful messaging systems, but they are designed for different primary purposes. The key difference lies in their architecture: Kafka is a distributed, replayable commit log, while RabbitMQ is a traditional message broker.

| Feature         | **Apache Kafka (Streaming Platform)**                                                               | **RabbitMQ (Message Broker)**                                                                   |
| --------------- | --------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Model**       | A durable, replayable log. Consumers pull data and track their own position (offset).               | A "smart" broker that pushes messages to consumers and removes them from queues after they are acknowledged. |
| **Data Retention** | Messages are retained based on a policy (e.g., 7 days) and can be re-read multiple times.          | Messages are deleted once consumed and acknowledged.                                            |
| **Throughput**  | Extremely high. Optimized for high-volume, sequential data streams (millions of messages/sec).      | Moderate. Optimized for complex routing, per-message guarantees, and worker queues.             |
| **Primary Goal**| To be a high-throughput backbone for real-time data pipelines, streaming analytics, and event sourcing. | To be a reliable intermediary for application communication, task distribution, and background jobs. |

**Use Case Example:**
*   **Kafka is ideal for:** A user activity tracking system for a large media website. Millions of "page view," "video play," and "click" events need to be ingested per minute and made available to multiple systems (real-time analytics, recommendation engines, data warehousing) now and in the future.
*   **RabbitMQ is ideal for:** A web application's background job processor. When a new user signs up, the web server sends a `SendWelcomeEmail` message to a RabbitMQ queue. A separate "worker" process consumes this message, sends the email, and then acknowledges the message, which removes it from the queue. This is a low-volume, task-oriented workload.

## 8. Shutdown Instructions

To stop and remove the Kafka container and its associated network, run the following command from the project root:

```sh
docker compose down -v
```