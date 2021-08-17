package com.rian.demo.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.rian.demo.dto.SampleEventRequestDTO;
import com.rian.demo.dto.SampleMessageDTO;
import com.rian.demo.enuns.SampleEventDescriptor;

import lombok.AllArgsConstructor;

@Service
@EnableBinding(SampleEventBindingService.class)
@AllArgsConstructor
public class SamplePublishService {

	@Autowired
	private SampleEventBindingService processEventBinding;

	public void publish(SampleEventRequestDTO eventRequestDTO) {
		this.send(SampleMessageDTO.builder()
				.id(1l)
				.eventInfo(eventRequestDTO.getEventInfo())
				.partition(UUID.randomUUID().toString())
				.build());
	}

	private void send(SampleMessageDTO messageDTO) {
		processEventBinding.masterOutput().send(MessageBuilder.withPayload(messageDTO)
				.setHeaderIfAbsent(SampleEventDescriptor.EVENTO_ID, SampleEventDescriptor.PROCESS_EVENT.getDescricao())
				.build());
	}

}
