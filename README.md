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
Arquivo que contém pares de principais do Kerberos e chaves criptografadas (que são derivadas da senha do Kerberos). Você pode usar um arquivo keytab para autenticar em vários sistemas remotos usando Kerberos sem inserir uma senha. No entanto, ao alterar sua senha Kerberos, você precisará recriar todos os seus keytabs.

```xml
/usr/sbin/kadmin.local -q 'addprinc -randkey kafka/krb5.ahmad.io@AHMAD.IO'
/usr/sbin/kadmin.local -q "ktadd -k /etc/security/keytabs/kafka_server.keytab kafka/krb5.ahmad.io@AHMAD.IO"

```

# Segurança Apache Kafka com Kerberos
Apache Kafka é uma camada intermediária interna que permite que seus sistemas de back-end compartilhem feeds de dados em tempo real uns com os outros por meio de tópicos Kafka. Com uma configuração padrão do Kafka, qualquer usuário ou aplicativo pode escrever qualquer mensagem em qualquer tópico, bem como ler dados de qualquer tópico . Conforme sua empresa avança em direção a um modelo de locação compartilhado, onde várias equipes e aplicativos usam o mesmo Kafka Cluster, ou seu Kafka Cluster começa a embarcar algumas informações críticas e confidenciais, você precisa implementar a segurança.

**SASL GSSAPI** é baseado no mecanismo de tíquete Kerberos, uma forma muito segura de fornecer autenticação. O Microsoft Active Directory é a implementação mais comum do Kerberos. SASL/GSSAPI é uma ótima escolha, pois permite que as empresas gerenciem a segurança a partir de seu servidor Kerberos.

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

# Referências
https://www.gta.ufrj.br/grad/99_2/marcos/kerberos.htm#introd
https://www.howtoforge.com/how-to-setup-kerberos-server-and-client-on-ubuntu-1804-lts/
https://linuxconfig.org/how-to-install-kerberos-kdc-server-and-client-on-ubuntu-18-04
https://medium.com/xenonstack-security/apache-kafka-security-with-kerberos-on-kubernetes-1a10d378b35d
https://docs.confluent.io/2.0.0/kafka/sasl.html

