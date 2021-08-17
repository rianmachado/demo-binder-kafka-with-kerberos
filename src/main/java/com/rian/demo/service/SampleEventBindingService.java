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
