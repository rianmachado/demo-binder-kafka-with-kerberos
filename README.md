# Conectando-se ao Kafka com Spring Cloud Stream, usando autenticação Kerberos

**Kerberos** é um protocolo desenvolvido para fornecer poderosa autenticação em aplicações usuário/servidor, onde ele funciona como a terceira parte neste processo, oferendo autenticação ao usuário.

**O que faz o Kerberos:**
Na vida real, usamos rotineiramente uma autenticação, na forma de (por exemplo) uma carteira de motorista. A carteira de motorista é fornecida por uma agência, na qual podemos supor confiável, e contém dados pessoais da pessoa como nome, endereço e data de nascimento. Além disto pode conter algumas restrições, como necessidade de óculos para dirigir, e mesmo algumas restrições implícitas (a data de nascimento pode ser usada para comprovar maioridade). Finalmente, a identificação tem um prazo de validade, a partir do qual é considerada inválida.

Para ser aceita como autenticação, algumas regras devem ser observadas: ao apresentar a carteira, não se pode ocultar parte dela (como foto ou data de nascimento). Além disto, quem verifica a autenticação deve reconhecer a agência que forneceu a autenticação como válida (uma carteira de motorista emitida no Brasil pode não ser aceita fora do território nacional).

Kerberos trabalha basicamente da mesma maneira. Ele é tipicamente usado quando um usuário em uma rede tenta fazer uso de um determinado serviço da rede e o serviço quer se assegurar de que o usuário é realmente quem ele diz que é. Para isto, o usuário apresenta um ticket, fornecido pelo Kerberos authenticator server (AS), assim como a carteira de motorista é fornecida pelo DETRAN. O serviço então examina o ticket para verificar a identidade do usuário. Se tudo estiver ok o usuário é aceito.

O ticket deve conter informações que garantam a identidade do usuário. Como usuário e serviço não ficam face a face uma foto não se aplica. O ticket deve demonstrar que o portador sabe alguma coisa que somente o verdadeiro usuário saberia, como uma senha. Além disto, devem existir mecanismo de segurança contra um atacante que "pegue" um ticket e use mais tarde.

**Setup Kerberos Server**
Para conhecer detalhes de como configurar um servidor Kerberos acesse: [Setup](https://www.howtoforge.com/how-to-setup-kerberos-server-and-client-on-ubuntu-1804-lts/).

**Keytab**
Arquivo que contém pares de principais do Kerberos e chaves criptografadas (que são derivadas da senha do Kerberos). Você pode usar um arquivo keytab para autenticar em vários sistemas remotos usando Kerberos sem inserir uma senha. No entanto, ao alterar sua senha Kerberos, você precisará recriar todos os seus keytabs.

```xml
/usr/sbin/kadmin.local -q 'addprinc -randkey kafka/krb5.ahmad.io@AHMAD.IO'
/usr/sbin/kadmin.local -q "ktadd -k /etc/security/keytabs/kafka_server.keytab kafka/krb5.ahmad.io@AHMAD.IO"

```


**Aplicativos Spring Cloud Stream** 

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
