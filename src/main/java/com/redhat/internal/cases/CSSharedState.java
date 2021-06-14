package com.redhat.internal.cases;

import org.springframework.stereotype.Component;

@Component
public class CSSharedState {
    
    private Long processId;

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}
}