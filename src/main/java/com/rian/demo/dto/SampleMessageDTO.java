package com.rian.demo.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleMessageDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private Long id;
	
	private String eventInfo;
	
	private String partition;

}
