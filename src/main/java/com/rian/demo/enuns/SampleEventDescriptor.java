package com.rian.demo.enuns;

public enum SampleEventDescriptor {
	
	PROCESS_EVENT("PROCESS_EVENT");
	
	public static final String EVENTO_ID = "evento";
	public static final String MASTER_IN = "masterInput";
	public static final String MASTER_OUT = "masterOutput";
	
	private String descricao;
	
	SampleEventDescriptor(String descricao){
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}
	

}
