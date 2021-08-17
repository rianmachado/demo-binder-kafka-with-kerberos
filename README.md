# Conectando-se ao Kafka com Spring Cloud Stream, usando autenticação Kerberos

Aplicativos Spring Cloud Stream consistem em um núcleo neutro de middleware. O Aplicativo se comunica com o mundo externo por meio de canais de entrada e saída injetados pelo Spring Cloud Stream. Esses canais são conectados ao brokers externos por meio de implementações de Binder. 

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
