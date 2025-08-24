Okay, here is the full, end-to-end project structure, all code, and dependencies for the event-driven e-commerce order processing system using **Java 21, Spring Boot 3.2.2, and RabbitMQ**.

---

### Project Setup and Directory Structure

First, create a parent directory for your project, for example, `event-driven-ecommerce-rabbitmq-java21`. Inside this, create the following structure:

```
event-driven-ecommerce-rabbitmq-java21/
├── docker-compose.yml
├── producer/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           └── producer/
│   │       │               ├── ProducerApplication.java
│   │       │               ├── config/
│   │       │               │   └── RabbitMQConfig.java
│   │       │               ├── controller/
│   │       │               │   └── OrderController.java
│   │       │               └── service/
│   │       │                   └── OrderService.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── consumer/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           └── consumer/
│   │       │               ├── ConsumerApplication.java
│   │       │               └── service/
│   │       │                   └── OrderConsumerService.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
└── README.md
```

---

### 1. `docker-compose.yml`

This file will set up the RabbitMQ container, making it easy to run the broker locally. Place this in the root `event-driven-ecommerce-rabbitmq-java21/` directory.

```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3.9-management # Uses RabbitMQ with management UI
    container_name: rabbitmq
    ports:
      - "5672:5672"  # AMQP port for applications to connect
      - "15672:15672" # Management UI port (accessible via http://localhost:15672)
    environment:
      RABBITMQ_DEFAULT_USER: guest # Default username
      RABBITMQ_DEFAULT_PASS: guest # Default password
    healthcheck: # Health check to ensure RabbitMQ is ready
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

---

### 2. Producer Application

Create a directory named `producer` inside your root project folder.

#### `producer/pom.xml`

This Maven `pom.xml` defines the producer's dependencies.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version> <!-- Spring Boot version compatible with Java 21 -->
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>producer</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>producer</name>
    <description>E-commerce Order Producer with Spring Boot and RabbitMQ</description>
    <properties>
        <java.version>21</java.version> <!-- Specifies Java 21 -->
    </properties>
    <dependencies>
        <!-- Spring Boot Web Starter for REST endpoints -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- Spring Boot AMQP Starter for RabbitMQ integration -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Spring RabbitMQ Test support -->
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

#### `producer/src/main/resources/application.properties`

Configuration for the Spring Boot application and RabbitMQ connection details.

```properties
# Server Port for the Producer REST API
server.port=8080

# RabbitMQ Connection Properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Custom RabbitMQ Configuration for the Order system
# This defines the names for the exchange, queue, and routing key
app.rabbitmq.exchange=order-events-exchange # A topic exchange to route messages
app.rabbitmq.queue=order-placed-queue      # The queue where order events will be stored
app.rabbitmq.routingkey=order.placed       # The routing key used to bind the queue to the exchange
```

#### `producer/src/main/java/com/example/producer/ProducerApplication.java`

The main entry point for the Spring Boot producer application.

```java
package com.example.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }
}
```

#### `producer/src/main/java/com/example/producer/config/RabbitMQConfig.java`

This class defines and configures the RabbitMQ exchange, queue, and their binding.

```java
package com.example.producer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Injecting values from application.properties
    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routingkey}")
    private String routingKey;

    /**
     * Defines a durable queue for order placed events.
     * A durable queue survives RabbitMQ restarts.
     */
    @Bean
    public Queue orderQueue() {
        return new Queue(queueName, true); // durable = true
    }

    /**
     * Defines a durable topic exchange.
     * A topic exchange routes messages to queues based on a routing key pattern.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(exchangeName, true, false); // durable = true, autoDelete = false
    }

    /**
     * Binds the queue to the exchange with a specific routing key.
     * Messages sent to the exchange with this routing key will be delivered to the queue.
     */
    @Bean
    public Binding binding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(routingKey);
    }
}
```

#### `producer/src/main/java/com/example/producer/controller/OrderController.java`

A REST controller to expose an endpoint for placing orders. This is the entry point for "OrderPlaced" events.

```java
package com.example.producer.controller;

import com.example.producer.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Handles POST requests to place a new order.
     * The order details are sent as a string (e.g., JSON payload).
     *
     * @param orderDetails A string representation of the order (e.g., JSON).
     * @return A confirmation message.
     */
    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody String orderDetails) {
        // In a real application, you'd parse orderDetails into an object
        // and perform validation before publishing.
        orderService.publishOrderPlacedEvent(orderDetails);
        return ResponseEntity.ok("Order placed and event published successfully!");
    }
}
```

#### `producer/src/main/java/com/example/producer/service/OrderService.java`

This service uses `RabbitTemplate` to send messages (events) to the configured RabbitMQ exchange.

```java
package com.example.producer.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate; // Spring's convenient way to send messages

    // Injected exchange and routing key from application.properties
    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routingkey}")
    private String routingKey;

    /**
     * Publishes an "OrderPlaced" event to the RabbitMQ exchange.
     *
     * @param orderDetails The details of the placed order, typically a JSON string.
     */
    public void publishOrderPlacedEvent(String orderDetails) {
        System.out.println("Producer: Publishing OrderPlaced event: " + orderDetails);
        // convertAndSend takes the exchange, routing key, and the message payload
        rabbitTemplate.convertAndSend(exchangeName, routingKey, orderDetails);
    }
}
```

---

### 3. Consumer Application

Create a directory named `consumer` inside your root project folder.

#### `consumer/pom.xml`

This Maven `pom.xml` defines the consumer's dependencies.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version> <!-- Spring Boot version compatible with Java 21 -->
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>consumer</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>consumer</name>
    <description>E-commerce Order Consumer with Spring Boot and RabbitMQ</description>
    <properties>
        <java.version>21</java.version> <!-- Specifies Java 21 -->
    </properties>
    <dependencies>
        <!-- Spring Boot Starter for core Spring features -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <!-- Spring Boot AMQP Starter for RabbitMQ integration -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Spring RabbitMQ Test support -->
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### `consumer/src/main/resources/application.properties`

Configuration for the Spring Boot consumer application and RabbitMQ connection details.

```properties
# Server Port for the Consumer (useful if it had web endpoints, otherwise optional)
server.port=8081

# RabbitMQ Connection Properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Custom RabbitMQ Configuration for the Order system
# This queue name must match the one the producer binds to
app.rabbitmq.queue=order-placed-queue
```

#### `consumer/src/main/java/com/example/consumer/ConsumerApplication.java`

The main entry point for the Spring Boot consumer application.

```java
package com.example.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
```

#### `consumer/src/main/java/com/example/consumer/service/OrderConsumerService.java`

This service contains the `@RabbitListener` method, which is automatically invoked when a message arrives on the configured queue.

```java
package com.example.consumer.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumerService {

    // Injected queue name from application.properties
    @Value("${app.rabbitmq.queue}")
    private String queueName;

    /**
     * Listens for messages on the 'order-placed-queue'.
     * When a message arrives, this method is automatically called to process it.
     *
     * @param orderDetails The message content, expected to be the order details string.
     */
    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void processOrderPlacedEvent(String orderDetails) {
        System.out.println("Consumer: Received OrderPlaced event: " + orderDetails);
        // Simulate order fulfillment logic
        System.out.println("Consumer: Simulating order fulfillment for: " + orderDetails);

        // In a real system, this would involve:
        // 1. Deserializing orderDetails into an Order object
        // 2. Updating a database (e.g., marking order as 'processing')
        // 3. Potentially interacting with other services (e.g., inventory, shipping)
        // 4. Publishing new events (e.g., OrderProcessed, InventoryUpdated)

        System.out.println("Consumer: Order fulfillment confirmed for: " + orderDetails);
        System.out.println("---------------------------------------------------------");
    }
}
```

---

### 4. `README.md`

Place this in the root `event-driven-ecommerce-rabbitmq-java21/` directory.

```markdown
# Event-Driven E-commerce Order Processing System (Java 21, Spring Boot, RabbitMQ)

This project demonstrates a simple event-driven architecture for an e-commerce order processing system. It consists of two Spring Boot applications (a producer and a consumer) communicating asynchronously via a RabbitMQ message broker.

**Technical Stack:**
*   **Java 21**: The latest LTS version of Java.
*   **Spring Boot 3.2.2**: Framework for building robust Java applications, compatible with Java 21.
*   **RabbitMQ**: An open-source message broker used for asynchronous communication.
*   **Maven**: Build automation tool.
*   **Docker / Docker Compose**: For easily setting up the RabbitMQ instance.

## Objective

To demonstrate the core components of an event-driven architecture:
*   **Producer**: Generates "OrderPlaced" events.
*   **Message Broker (RabbitMQ)**: Routes these events reliably.
*   **Consumer**: Listens for and processes these events by simulating order fulfillment.

## Requirements

*   A producer generates an "OrderPlaced" event when a customer places an order (via a REST API).
*   RabbitMQ routes the event to a queue.
*   A consumer listens for these events and processes them by simulating order fulfillment (e.g., logging order details and confirming fulfillment).

---

## Core Concept Questions (Answers)

### 1. Explain what an event is in Event-Driven Architecture giving a real-world example outside of technology.

In Event-Driven Architecture (EDA), an **event** is a significant occurrence or a change in state within a system. It is a factual record of "something that happened." Events are typically immutable and carry information about what occurred, when it happened, and sometimes the state changes involved. They are a declaration that a particular action or change has taken place, without necessarily implying a direct command or expectation of how that event should be handled.

**Real-world example outside of technology:**
Consider a **weather station detecting a sudden drop in temperature**.
*   **Event:** "TemperatureDropped"
*   **Information contained:** Current temperature, previous temperature, timestamp, location.
*   This event simply states a fact that occurred. It doesn't tell anyone what to do. Various systems can then *react* to this event:
    *   A "Frost Alert System" might send SMS warnings to farmers.
    *   A "Smart Home System" might turn on heating in connected homes.
    *   A "Road Gritting System" might dispatch trucks to spread salt.
Each system acts independently based on the same factual event.

### 2. Compare Event-Driven Architecture (EDA) with Request-Response Architecture. What are the key advantages and disadvantages of each approach?

**Request-Response Architecture (Traditional/Synchronous):**
In this model, a client sends a request to a server and then *waits* for a response. The client is blocked until the server processes the request and sends back a reply.

*   **Key Advantages:**
    *   **Simplicity for simple interactions:** Easy to understand and implement for direct, synchronous operations where an immediate result is needed.
    *   **Predictable flow:** The execution path is usually straightforward and easy to debug.
    *   **Direct feedback:** The client receives an immediate success or failure message.
*   **Key Disadvantages:**
    *   **Tight Coupling:** The client needs to know the server's endpoint and often its interface. A change in the server's API can break the client.
    *   **Limited Scalability:** If the server is slow or busy, the client is blocked, leading to performance bottlenecks. Scaling often requires scaling the entire stack synchronously.
    *   **Lack of Resilience:** If the server is unavailable, the client request fails immediately. There's no inherent mechanism for retries or deferred processing.
    *   **Chained Failures:** A failure in one service can easily cascade and cause failures in other dependent services.

**Event-Driven Architecture (EDA / Asynchronous):**
In this model, producers publish events to a message broker, and consumers subscribe to these events. Producers do not wait for consumers to process the events. Communication is decoupled and asynchronous.

*   **Key Advantages:**
    *   **Loose Coupling:** Producers and consumers are highly decoupled. They only need to know the event format and the broker's address, not each other's existence or location. This allows for independent development, deployment, and scaling.
    *   **High Scalability:** Consumers can be scaled independently based on event volume. You can add more consumer instances to handle increased load without affecting producers.
    *   **Improved Resilience & Fault Tolerance:** If a consumer is down, the events are typically stored in the message broker and processed when the consumer recovers. The producer is not affected by consumer failures.
    *   **Asynchronous Processing:** Long-running tasks can be offloaded, allowing the primary request thread to respond quickly.
    *   **Flexibility & Extensibility:** New consumers can easily be added to react to existing events without modifying producers, enabling new features or integrations with minimal disruption.
*   **Key Disadvantages:**
    *   **Increased Complexity:** Distributed systems are harder to design, debug, and monitor. Tracing an event's journey across multiple services can be challenging.
    *   **Eventual Consistency:** Data consistency across different services might not be immediate. Services react to events at their own pace, leading to eventual consistency rather than immediate consistency.
    *   **Ordering Guarantees:** Ensuring strict event ordering can be complex, especially with multiple consumers or partitions.
    *   **Debugging/Testing:** Harder to simulate end-to-end flows for testing and debugging due to the asynchronous nature.

### 3. While developing an e-commerce application, how would you use Event-Driven Architecture (EDA) to manage the following scenarios?

**Scenario: Placing an Order**

*   When a customer clicks "Place Order," the **Frontend** sends a request to the **Order Service**.
*   The **Order Service** validates the order, persists it to its database (e.g., with a "Pending" status), and then publishes an `OrderPlaced` event to the message broker. The Order Service immediately returns a success response to the customer.
*   The `OrderPlaced` event contains details like `orderId`, `customerId`, `itemsPurchased`, `totalAmount`, etc.

**Scenario: Sending a Confirmation Email**

*   A **Notification Service** (a consumer) subscribes to the `OrderPlaced` event from the message broker.
*   Upon receiving an `OrderPlaced` event, the Notification Service extracts the `customerId` and `orderId` (and potentially other details like `customerEmail` from a Customer service or the event itself).
*   It then composes and sends a confirmation email to the customer. This process is asynchronous and decoupled from the order placement. If the email service fails temporarily, the order is still placed.

**Scenario: Updating Inventory**

*   An **Inventory Service** (another consumer) also subscribes to the `OrderPlaced` event.
*   When it receives an `OrderPlaced` event, it extracts the `itemsPurchased` details.
*   It then reduces the stock levels for each item in its own inventory database.
*   If an item is out of stock or there's a problem, it might publish an `InventoryUpdateFailed` event or an `OrderCancellationRequested` event back to the broker, which other services (like the Order Service) could consume.

### 4. Why is Event-Driven Architecture considered a good fit for Microservices and Cloud-Native systems? Provide at least two strong reasons.

EDA is exceptionally well-suited for Microservices and Cloud-Native systems due to several inherent properties:

1.  **Enforces Loose Coupling and Independent Deployability:**
    *   In a microservices architecture, services should ideally be developed, deployed, and scaled independently without direct dependencies on other services. EDA achieves this by making services communicate indirectly through a message broker. A producer publishes an event without knowing or caring which, or how many, consumers will react to it. This decoupling is crucial for microservices, as it prevents tight integration issues, allows teams to work autonomously, and reduces the "blast radius" of changes or failures. Cloud-native systems benefit from this as components can be updated or replaced dynamically without service interruptions.

2.  **Enhances Scalability and Resilience:**
    *   Cloud-native systems are designed for elastic scaling and fault tolerance. EDA naturally supports this:
        *   **Scalability:** If a particular business process (e.g., inventory updates after an order) experiences high load, you can simply add more instances of the corresponding consumer service. The message broker distributes the events among available consumer instances, allowing horizontal scaling without affecting other services.
        *   **Resilience:** If a consumer service temporarily fails or becomes overloaded, the message broker (like RabbitMQ) will buffer the events. Once the consumer recovers, it can resume processing from where it left off, ensuring no data loss and preventing cascading failures. The producer remains unaffected and can continue publishing events. This "fire-and-forget" mechanism is critical for robust, highly available cloud-native applications.

### 5. How does EDA help in building scalable systems? Write two real-world use cases where EDA is better than traditional monolithic systems.

EDA significantly aids in building scalable systems by decoupling components and enabling asynchronous processing. When components are decoupled, they can be scaled independently based on their specific load requirements. If a particular task (like sending emails) becomes a bottleneck, you can simply add more instances of the email-sending service (consumer) to process messages faster, without needing to scale the entire application or impact other services. The message broker acts as a buffer, smoothing out spikes in traffic.

**Two real-world use cases where EDA is better than traditional monolithic systems:**

1.  **Financial Transaction Processing (e.g., Stock Trading Platform):**
    *   **Monolithic Approach:** A single, large application handles everything from order placement, trade execution, portfolio updates, risk assessment, and regulatory reporting. When a high volume of trades comes in, the entire system can bog down. If the risk assessment module is slow, it blocks trade execution, causing latency and potential financial losses.
    *   **EDA Approach:**
        *   When a trade order is placed, an `OrderPlaced` event is published.
        *   An `Execution Service` consumes this event to match buy/sell orders.
        *   An `Account Service` consumes the event to update user balances.
        *   A `Risk Management Service` consumes the event for real-time risk assessment.
        *   A `Reporting Service` consumes the event for compliance logging.
        *   Each service can scale independently. If trading volume surges, you can spin up more instances of the Execution Service without affecting the Risk Management or Reporting Services. This ensures high throughput, low latency for critical path, and allows parallel processing of complex tasks, making the system highly scalable and resilient to individual component failures.

2.  **Internet of Things (IoT) Data Ingestion and Processing:**
    *   **Monolithic Approach:** A single application attempts to receive data from millions of IoT devices, process it, store it, and trigger actions. This system would quickly become a bottleneck, struggle with inconsistent data rates, and be highly vulnerable to a single point of failure.
    *   **EDA Approach:**
        *   Each IoT device publishes `SensorData` events (e.g., temperature, pressure, location) to a message broker.
        *   A `Data Ingestion Service` consumes these events, cleans and validates the data, and stores it in a raw data lake.
        *   A `Real-time Analytics Service` consumes the same events to monitor for anomalies or trigger immediate alerts.
        *   A `Command & Control Service` consumes specific events (e.g., "AlertTriggered") to send commands back to devices.
        *   The message broker efficiently handles millions of concurrent connections and acts as a buffer for high-volume, bursty data. Individual processing services can be scaled out instantly to handle peak loads, ensuring no data is lost and enabling real-time insights without overwhelming the entire system.

---

## How to Run This Project

**Prerequisites:**
*   **Java Development Kit (JDK) 21**: Make sure you have Java 21 installed and configured as your `JAVA_HOME`.
*   **Apache Maven**: For building and managing project dependencies.
*   **Docker Desktop**: To run RabbitMQ as a container.

**Steps:**

1.  **Clone or Download the Project:**
    Ensure your project structure matches the one described at the beginning.

2.  **Start RabbitMQ using Docker Compose:**
    Open a terminal or command prompt in the root directory of your project (`event-driven-ecommerce-rabbitmq-java21/`) where `docker-compose.yml` is located.
    ```bash
    docker-compose up -d
    ```
    This will download the RabbitMQ image (if not present) and start the container in the background. You can access the RabbitMQ Management UI at `http://localhost:15672` (default user: `guest`, password: `guest`).

3.  **Build and Run the Producer Application:**
    Navigate into the `producer` directory:
    ```bash
    cd producer
    ```
    Build and run the application using Maven Spring Boot plugin:
    ```bash
    ./mvnw spring-boot:run
    ```
    (On Windows, use `mvnw.cmd spring-boot:run`)
    The producer application will start on `http://localhost:8080`.

4.  **Build and Run the Consumer Application:**
    Open a **new terminal window**. Navigate into the `consumer` directory:
    ```bash
    cd consumer
    ```
    Build and run the application:
    ```bash
    ./mvnw spring-boot:run
    ```
    (On Windows, use `mvnw.cmd spring-boot:run`)
    The consumer application will start on `http://localhost:8081` (though it primarily listens to RabbitMQ).

5.  **Place an Order (Send an Event):**
    From yet another terminal window, or using a tool like Postman/Insomnia, send a POST request to the producer's API.
    ```bash
    curl -X POST -H "Content-Type: application/json" -d '{"orderId": "ORD-001", "customerId": "user-123", "items": [{"productId": "P001", "quantity": 1}], "total": 99.99}' http://localhost:8080/api/orders
    ```
    You can send this command multiple times with different order details to see more events being processed.

**Expected Output:**

*   **Producer Terminal:** You will see logs indicating that the "OrderPlaced" event has been published to RabbitMQ.
    ```
    Producer: Publishing OrderPlaced event: {"orderId": "ORD-001", ...}
    ```
*   **Consumer Terminal:** You will see logs indicating that the "OrderPlaced" event has been received and processed.
    ```
    Consumer: Received OrderPlaced event: {"orderId": "ORD-001", ...}
    Consumer: Simulating order fulfillment for: {"orderId": "ORD-001", ...}
    Consumer: Order fulfillment confirmed for: {"orderId": "ORD-001", ...}
    ---------------------------------------------------------
    ```

6.  **Stop the Applications and RabbitMQ:**
    *   To stop the Spring Boot applications, go to their respective terminal windows and press `Ctrl+C`.
    *   To stop and remove the RabbitMQ container, go back to the root project directory and run:
        ```bash
        docker-compose down
        ```