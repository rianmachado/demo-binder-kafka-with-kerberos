package com.rian.demo.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.rian.demo.dto.SampleMessageDTO;
import com.rian.demo.enuns.SampleEventDescriptor;

import lombok.AllArgsConstructor;

@Component
@EnableBinding(SampleEventBindingService.class)
@AllArgsConstructor
public class SampleSubscribeService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleSubscribeService.class);

	@StreamListener(target = SampleEventDescriptor.MASTER_IN, condition = "headers['evento']=='PROCESS_EVENT'")
	public void listenerToProcessEvent(Message<SampleMessageDTO> msg) {
		LOGGER.info("Dado recebido", msg);
	}

}
