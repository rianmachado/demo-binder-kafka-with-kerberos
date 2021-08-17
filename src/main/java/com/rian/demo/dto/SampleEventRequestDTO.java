package com.rian.demo.dto;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

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
public class SampleEventRequestDTO implements Serializable {
	private static final long serialVersionUID = -8086218137376013439L;

	@NotBlank
	private String eventInfo;

}
