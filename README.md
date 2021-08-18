# Conectando-se ao Kafka com Spring Cloud Stream, usando autenticação Kerberos

**Kerberos** é um protocolo desenvolvido para fornecer poderosa autenticação em aplicações usuário/servidor, onde ele funciona como a terceira parte neste processo, oferendo autenticação ao usuário.

**O que faz o Kerberos:** </br>
Na vida real, usamos rotineiramente uma autenticação, na forma de (por exemplo) uma carteira de motorista. A carteira de motorista é fornecida por uma agência, na qual podemos supor confiável, e contém dados pessoais da pessoa como nome, endereço e data de nascimento. Além disto pode conter algumas restrições, como necessidade de óculos para dirigir, e mesmo algumas restrições implícitas (a data de nascimento pode ser usada para comprovar maioridade). Finalmente, a identificação tem um prazo de validade, a partir do qual é considerada inválida.

Para ser aceita como autenticação, algumas regras devem ser observadas: ao apresentar a carteira, não se pode ocultar parte dela (como foto ou data de nascimento). Além disto, quem verifica a autenticação deve reconhecer a agência que forneceu a autenticação como válida (uma carteira de motorista emitida no Brasil pode não ser aceita fora do território nacional).

Kerberos trabalha basicamente da mesma maneira. Ele é tipicamente usado quando um usuário em uma rede tenta fazer uso de um determinado serviço da rede e o serviço quer se assegurar de que o usuário é realmente quem ele diz que é. Para isto, o usuário apresenta um ticket, fornecido pelo Kerberos authenticator server (AS), assim como a carteira de motorista é fornecida pelo DETRAN. O serviço então examina o ticket para verificar a identidade do usuário. Se tudo estiver ok o usuário é aceito.

O ticket deve conter informações que garantam a identidade do usuário. Como usuário e serviço não ficam face a face uma foto não se aplica. O ticket deve demonstrar que o portador sabe alguma coisa que somente o verdadeiro usuário saberia, como uma senha. Além disto, devem existir mecanismo de segurança contra um atacante que "pegue" um ticket e use mais tarde.

**Setup Kerberos Server**</br>
Para conhecer detalhes de como configurar um servidor Kerberos acesse: [Setup](https://www.howtoforge.com/how-to-setup-kerberos-server-and-client-on-ubuntu-1804-lts/).

**Keytab**</br>
Arquivo que contém o Kerberos Principal(identidade única) e chaves criptografadas que são derivadas da senha do Kerberos. Você pode usar um arquivo keytab para se autenticar em vários sistemas remotos usando Kerberos sem inserir uma senha. No entanto, ao alterar sua senha Kerberos, você precisará recriar todos os seus keytabs.
```xml
/usr/sbin/kadmin.local -q 'addprinc -randkey kafka/krb5.ahmad.io@AHMAD.IO'
/usr/sbin/kadmin.local -q "ktadd -k /etc/security/keytabs/kafka_server.keytab kafka/krb5.ahmad.io@AHMAD.IO"

```

# Segurança Apache Kafka com Kerberos
Apache Kafka é uma camada intermediária interna que permite aos sistemas de back-end compartilharem dados em tempo real por meio de tópicos. A configuração padrão do Kafka, permite que qualquer usuário ou aplicativo possa escrever mensagens em qualquer tópico, bem como ler dados. Conforme sua empresa avança em direção a um modelo onde várias equipes e aplicativos usam o mesmo Kafka Cluster, algumas informações críticas e confidenciais ganham protagonismo. Você precisa implementar a segurança.

**SASL/GSSAPI** é uma ótima escolha, pois permite que as empresas gerenciem a segurança a partir de seu servidor Kerberos.

- Adicione um arquivo JAAS(<KAFKA_HOME>/config/kafka_server_jaas.conf) apontando para o keytab criado:

```xml
KafkaServer {
    com.sun.security.auth.module.Krb5LoginModule required
    useTicketCache=true
    serviceName=kafka
    useKeyTab=true
    storeKey=true
    keyTab="/etc/security/keytabs/kafka_server.keytab"
    principal="kafka/krb5.ahmad.io@AHMAD.IO";
};
KafkaClient {
    com.sun.security.auth.module.Krb5LoginModule required
    useTicketCache=true
    serviceName=kafka
    useKeyTab=true
    storeKey=true
    keyTab="/etc/security/keytabs/kafka_server.keytab"
    principal="kafka/krb5.ahmad.io@AHMAD.IO";
};
```
- Passe para a JVM subjacente ao broker Kafka os seguintes parametros:
```xml
export KAFKA_OPTS="-Djava.security.krb5.conf=/etc/krb5.conf -Djava.security.auth.login.config=/home/rian/kafka_2.12-2.8.0/config/kafka_server_jaas.conf"
```
- Configurando SASL in <KAFKA_HOME>/config/server.properties
```xml
listeners=SASL_PLAINTEXT://krb5.ahmad.io:9092
security.inter.broker.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=kafka
sasl.mechanism.inter.broker.protocol=GSSAPI
sasl.enabled.mechanism=GSS
```


# Aplicativos Spring Cloud Stream
Consistem em um núcleo neutro de middleware. O Aplicativo se comunica com o mundo externo por meio de canais de entrada e saída injetados pelo Spring Cloud Stream. Esses canais são conectados ao brokers externos por meio de implementações de Binder.

![](https://docs.spring.io/spring-cloud-stream/docs/1.0.0.RC1/reference/html/images/SCSt-with-binder.png)

```xml
bindings.masterOutput:
  destination: demo
  binder: kafka1
  group: group1
  contentType: application/json
  producer.partitionKeyExpression: payload.id
  producer.partitionCount: 1
  producer.requiredGroups: group1
bindings.masterInput:
  destination: demo
  binder: kafka1
  group: group1
  contentType: application/json

```
```java
package com.rian.demo.service;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

import com.rian.demo.enuns.SampleEventDescriptor;

public interface SampleEventBindingService {
	
	@Input(SampleEventDescriptor.MASTER_IN)
	SubscribableChannel masterInput();
	
	@Output(SampleEventDescriptor.MASTER_OUT)
	MessageChannel masterOutput();

}
```
**Conexões seguras(SASL_SSL) entre client e brokers**
```xml
spring.cloud.stream:
  defaultBinder: kafka1
  binders:
    kafka1:
      type: kafka
      environment:
        spring:
          kafka:
            bootstrap-servers: krb5.ahmad.io:9092
            jaas:
              enabled: true
              options:
                useKeyTab: true
                keyTab: /etc/security/keytabs/kafka_server.keytab
                storeKey: true
                useTicketCache: false
                serviceName: kafka
                principal: kafka/krb5.ahmad.io@AHMAD.IO
              control-flag: required
            properties:
              security:
                protocol: SASL_PLAINTEXT
              sasl:
                mechanism: GSSAPI
                kerberos:
                  service:
                    name: kafka
```
```xml
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.6.RELEASE)

2021-08-17 20:34:04.029  INFO 17185 --- [  restartedMain] com.rian.demo.DemoApplication            : Starting DemoApplication on krb5.ahmad.io with PID 17185 (/home/rian/demo-binder-kafka-with-kerberos/target/classes started by rian in /home/rian/demo-binder-kafka-with-kerberos)
2021-08-17 20:34:04.034  INFO 17185 --- [  restartedMain] com.rian.demo.DemoApplication            : No active profile set, falling back to default profiles: default
2021-08-17 20:34:04.169  INFO 17185 --- [  restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : Devtools property defaults active! Set 'spring.devtools.add-properties' to 'false' to disable
2021-08-17 20:34:04.170  INFO 17185 --- [  restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : For additional web related logging consider setting the 'logging.level.web' property to 'DEBUG'
2021-08-17 20:34:06.789  INFO 17185 --- [  restartedMain] faultConfiguringBeanFactoryPostProcessor : No bean named 'errorChannel' has been explicitly defined. Therefore, a default PublishSubscribeChannel will be created.
2021-08-17 20:34:06.797  INFO 17185 --- [  restartedMain] faultConfiguringBeanFactoryPostProcessor : No bean named 'taskScheduler' has been explicitly defined. Therefore, a default ThreadPoolTaskScheduler will be created.
2021-08-17 20:34:06.803  INFO 17185 --- [  restartedMain] faultConfiguringBeanFactoryPostProcessor : No bean named 'integrationHeaderChannelRegistry' has been explicitly defined. Therefore, a default DefaultHeaderChannelRegistry will be created.
2021-08-17 20:34:06.910  INFO 17185 --- [  restartedMain] trationDelegate$BeanPostProcessorChecker : Bean 'integrationChannelResolver' of type [org.springframework.integration.support.channel.BeanFactoryChannelResolver] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2021-08-17 20:34:06.918  INFO 17185 --- [  restartedMain] trationDelegate$BeanPostProcessorChecker : Bean 'integrationDisposableAutoCreatedBeans' of type [org.springframework.integration.config.annotation.Disposables] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2021-08-17 20:34:06.951  INFO 17185 --- [  restartedMain] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.integration.config.IntegrationManagementConfiguration' of type [org.springframework.integration.config.IntegrationManagementConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2021-08-17 20:34:07.629  INFO 17185 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2021-08-17 20:34:07.650  INFO 17185 --- [  restartedMain] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2021-08-17 20:34:07.652  INFO 17185 --- [  restartedMain] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.39]
2021-08-17 20:34:07.848  INFO 17185 --- [  restartedMain] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2021-08-17 20:34:07.849  INFO 17185 --- [  restartedMain] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 3678 ms
2021-08-17 20:34:10.267  INFO 17185 --- [  restartedMain] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2021-08-17 20:34:10.717  INFO 17185 --- [  restartedMain] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService 'taskScheduler'
2021-08-17 20:34:10.744  INFO 17185 --- [  restartedMain] onConfiguration$FunctionBindingRegistrar : Functional binding is disabled due to the presense of @EnableBinding annotation in your configuration
2021-08-17 20:34:11.100  INFO 17185 --- [  restartedMain] o.s.b.d.a.OptionalLiveReloadServer       : LiveReload server is running on port 35729
2021-08-17 20:34:11.138  INFO 17185 --- [  restartedMain] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
2021-08-17 20:34:11.395  INFO 17185 --- [  restartedMain] o.s.c.s.m.DirectWithAttributesChannel    : Channel 'application.masterInput' has 1 subscriber(s).
2021-08-17 20:34:11.406  INFO 17185 --- [  restartedMain] o.s.i.endpoint.EventDrivenConsumer       : Adding {logging-channel-adapter:_org.springframework.integration.errorLogger} as a subscriber to the 'errorChannel' channel
2021-08-17 20:34:11.408  INFO 17185 --- [  restartedMain] o.s.i.channel.PublishSubscribeChannel    : Channel 'application.errorChannel' has 1 subscriber(s).
2021-08-17 20:34:11.409  INFO 17185 --- [  restartedMain] o.s.i.endpoint.EventDrivenConsumer       : started bean '_org.springframework.integration.errorLogger'
2021-08-17 20:34:11.411  INFO 17185 --- [  restartedMain] o.s.c.s.binder.DefaultBinderFactory      : Creating binder: kafka1
2021-08-17 20:34:11.488  INFO 17185 --- [  restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : Devtools property defaults active! Set 'spring.devtools.add-properties' to 'false' to disable
2021-08-17 20:34:11.553  INFO 17185 --- [  restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : Devtools property defaults active! Set 'spring.devtools.add-properties' to 'false' to disable
2021-08-17 20:34:12.090  INFO 17185 --- [  restartedMain] o.s.c.s.binder.DefaultBinderFactory      : Caching the binder: kafka1
2021-08-17 20:34:12.091  INFO 17185 --- [  restartedMain] o.s.c.s.binder.DefaultBinderFactory      : Retrieving cached binder: kafka1
2021-08-17 20:34:12.345  INFO 17185 --- [  restartedMain] o.s.c.s.b.k.p.KafkaTopicProvisioner      : Using kafka topic for outbound: masterOutput
2021-08-17 20:34:12.355  INFO 17185 --- [  restartedMain] o.a.k.clients.admin.AdminClientConfig    : AdminClientConfig values:
	bootstrap.servers = [krb5.ahmad.io:9092]
	client.dns.lookup = default
	client.id = 
	connections.max.idle.ms = 300000
	default.api.timeout.ms = 60000
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 2147483647
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = kafka
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = SASL_PLAINTEXT
	security.providers = null
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLSv1.2
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS

2021-08-17 20:34:12.632  INFO 17185 --- [  restartedMain] o.a.k.c.s.authenticator.AbstractLogin    : Successfully logged in.
2021-08-17 20:34:12.644  INFO 17185 --- [mad.io@AHMAD.IO] o.a.k.c.security.kerberos.KerberosLogin  : [Principal=kafka/krb5.ahmad.io@AHMAD.IO]: TGT refresh thread started.
2021-08-17 20:34:12.664  INFO 17185 --- [mad.io@AHMAD.IO] o.a.k.c.security.kerberos.KerberosLogin  : [Principal=kafka/krb5.ahmad.io@AHMAD.IO]: TGT valid starting at: Tue Aug 17 20:34:12 UTC 2021
2021-08-17 20:34:12.665  INFO 17185 --- [mad.io@AHMAD.IO] o.a.k.c.security.kerberos.KerberosLogin  : [Principal=kafka/krb5.ahmad.io@AHMAD.IO]: TGT expires: Wed Aug 18 06:34:12 UTC 2021
2021-08-17 20:34:12.667  INFO 17185 --- [mad.io@AHMAD.IO] o.a.k.c.security.kerberos.KerberosLogin  : [Principal=kafka/krb5.ahmad.io@AHMAD.IO]: TGT refresh sleeping until: Wed Aug 18 05:04:04 UTC 2021
2021-08-17 20:34:12.755  WARN 17185 --- [  restartedMain] o.a.k.clients.admin.AdminClientConfig    : The configuration 'sasl.kerberos.service.name' was supplied but isn't a known config.
2021-08-17 20:34:12.760  INFO 17185 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 2.5.1
2021-08-17 20:34:12.760  INFO 17185 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 0efa8fb0f4c73d92
2021-08-17 20:34:12.762  INFO 17185 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1629232452758
2021-08-17 20:34:13.643  WARN 17185 --- [mad.io@AHMAD.IO] o.a.k.c.security.kerberos.KerberosLogin  : [Principal=kafka/krb5.ahmad.io@AHMAD.IO]: TGT renewal thread has been interrupted and will exit.
2021-08-17 20:34:13.652  INFO 17185 --- [  restartedMain] o.a.k.clients.producer.ProducerConfig    : ProducerConfig values: 

```


# Referências
* https://www.gta.ufrj.br/grad/99_2/marcos/kerberos.htm#introd
* https://www.howtoforge.com/how-to-setup-kerberos-server-and-client-on-ubuntu-1804-lts/
* https://linuxconfig.org/how-to-install-kerberos-kdc-server-and-client-on-ubuntu-18-04
* https://medium.com/xenonstack-security/apache-kafka-security-with-kerberos-on-kubernetes-1a10d378b35d
* https://docs.confluent.io/2.0.0/kafka/sasl.html

