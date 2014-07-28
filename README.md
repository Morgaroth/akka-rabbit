## AkkaRabbit

AkkaRabbit is an asynchronous RabbitMQ client based on Akka.

### Dependencies

AkkaRabbit is on Maven Central, under the GroupID of `com.coiney`. It uses akka 2.3.4 and is released for scala 2.10 and 2.11
```
<dependency>
    <groupId>com.coiney</groupId>
    <artifactId>akka-rabbit-core</artifactId>
    <version>0.2</version>
</dependency>
```

For people using SBT, add the following to your dependencies to get you started:
```
"com.coiney"        %% "akka-rabbit-core"       % "0.2"
```

### Usage

#### Connecting to RabbitMQ
The RabbitMQ connection configuration is done through Typesafe Configuration Library[*](https://github.com/typesafehub/config) (see [reference](https://github.com/Coiney/akka-rabbit/blob/develop/akka-rabbit-core/src/main/resources/reference.conf)). Once that is configured, you can connect RabbitMQ as follows:
```
import akka.actor.ActorSystem
import com.coiney.akka.rabbit.{QueueConfig, RabbitSystem}
import com.coiney.akka.rabbit.protocol._

object Main extends App {
  implicit val system = ActorSystem()
  
  val rabbitSystem = RabbitSystem()
  val connection: ActorRef = rabbitSystem waitFor rabbitSystem.createConnection()
  
  // example for a producer
  val producer: ActorRef = rabbitSystem waitFor rabbitSystem.createProducer(connection)
  producer ! DeclareQueue(QueueConfig("test.queue"))
  producer ! Publish("", "test.queue", "Hello World!".getBytes("UTF-8"))
  
  system.shutDown()
}
```

You can find additional examples under [akka-rabbit-example](https://github.com/Coiney/akka-rabbit/tree/develop/akka-rabbit-example).

### License

Licensed under the BSD License. See the LICENSE file for details.