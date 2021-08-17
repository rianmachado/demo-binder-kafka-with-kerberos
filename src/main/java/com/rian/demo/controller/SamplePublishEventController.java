package com.rian.demo.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rian.demo.dto.SampleEventRequestDTO;
import com.rian.demo.service.SamplePublishService;

@RestController
@RequestMapping("/api/v1/demo-publish/")
public class SamplePublishEventController {

	@Autowired
	private SamplePublishService samplePublishService;

	@PostMapping
	public ResponseEntity<SampleEventRequestDTO> create(@RequestBody @Valid SampleEventRequestDTO sample) {
		samplePublishService.publish(sample);
		return ResponseEntity.accepted().build();
	}

}
