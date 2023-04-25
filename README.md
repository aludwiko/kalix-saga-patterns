# Kalix Saga patterns

This project contains Saga patterns examples in Kalix ecosystem. A basic understanding of Kalix components is required to follow to code. To
understand more about these components,
see [Developing services](https://docs.kalix.io/services/) and check
Spring-SDK [official documentation](https://docs.kalix.io/spring/index.html). Examples can be
found [here](https://github.com/lightbend/kalix-jvm-sdk/tree/main/samples) in the folders with "spring" in their name.

Use Maven to build your project:

```shell
mvn compile
```

To run the example locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the
proxy for a locally running application.
It also contains the configuration to start a local Google Pub/Sub emulator that the Kalix proxy will connect to.
To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn spring-boot:run
```

With both the proxy and your application running, once you have defined endpoints they should be available at `http://localhost:9000`.

## Testing application

Create wallet

```shell
curl -X POST http://localhost:9000/wallet/1/create/100  
```

Deposit funds

```shell
curl -X PATCH http://localhost:9000/wallet/1/deposit/50 \
  --header "Content-Type: application/json" \
  --data '{"amount": "50", "commandId": 234}'  
```

Get wallet

```shell
curl http://localhost:9000/wallet/1
```

Create cinema show

```shell
curl http://localhost:9000/cinema-show/show1 \
  -X POST \
  --header "Content-Type: application/json" \
  --data '{"title": "Pulp Fiction", "maxSeats": 10}'
```

Get cinema show

```shell
curl http://localhost:9000/cinema-show/show1
```

Make reservation

```shell
curl http://localhost:9000/cinema-show/show1/reserve \
  -X PATCH \
  --header "Content-Type: application/json" \
  --data '{"walletId": "1", "reservationId": "123", "seatNumber": 3}'
```

Verify wallet balance

```shell
curl http://localhost:9000/wallet/1
```

Verify seat status

```shell
curl http://localhost:9000/cinema-show/show1/seat-status/3
```

# Deploy

To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a project and then deploy your service into the project either by using `mvn deploy` which
will also conveniently package and publish your docker image prior to deployment, or by first packaging and
publishing the docker image through `mvn clean package docker:push -DskipTests` and then deploying the image
through the `kalix` CLI.
