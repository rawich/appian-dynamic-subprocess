package com.persistent.plugins.dynamic_subprocess;

import java.util.Locale;

import org.apache.log4j.Logger;

import com.appian.plugins.dynamic_subprocess.DynamicSubprocess;
import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.ServiceContextFactory;
import com.appiancorp.suiteapi.common.ServiceLocator;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessExecutionService;
import com.appiancorp.suiteapi.process.ProcessModelProperties;
import com.appiancorp.suiteapi.process.ProcessProperties;
import com.appiancorp.suiteapi.process.TaskProperties;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.ActivityExecutionMetadata;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;

public class DynamicAsyncSubprocess extends AppianSmartService {
	private static final Logger LOG = Logger.getLogger(DynamicSubprocess.class);
	
	private final SmartServiceContext smartServiceContext;
	private Long modelId;
	private String modelUUID;
	private Long subProcessId;
	

	public DynamicAsyncSubprocess(SmartServiceContext smartServiceContext) {
		super();
		this.smartServiceContext = smartServiceContext;
	}

	@Override
	public void run() throws SmartServiceException {
		ServiceContext sc = ServiceContextFactory.getServiceContext(smartServiceContext.getUsername());
		
		ProcessExecutionService pes = ServiceLocator.getProcessExecutionService(asc);
		ProcessDesignService apds = ServiceLocator.getProcessDesignService(asc);
  
		ProcessDesignService pds = ServiceLocator.getProcessDesignService(sc);

		if (modelId == null) {
			try {
				modelId = apds.getProcessModelByUuid(modelUUID).getId();
			} catch (Exception e) {
				LOG.error(e,e);
				throw createException(e, "error.invalidUUID");
			}
		}
  

	}

}
