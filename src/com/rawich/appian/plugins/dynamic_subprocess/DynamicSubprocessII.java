package com.rawich.appian.plugins.dynamic_subprocess;

import org.apache.log4j.Logger;

import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.ServiceContextFactory;

import com.appiancorp.suiteapi.common.ServiceLocator;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessExecutionService;
import com.appiancorp.suiteapi.process.ProcessStartConfig;
import com.appiancorp.suiteapi.process.ProcessVariable;
import com.appiancorp.suiteapi.process.ProcessVariableInstance;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.NamedTypedValue;

/**
 * 
 * @author Originally created by Ryan Gates (Appian Corp - April 2013) / Modified by Rawich Poomrin (September 2015)
 * @version 2.0.1
 * This version addresses 2 issues:
 *   1) The node will fail to start sub-process if the Run-As user is not a system administrator and UUID is used to identify the sub-process.
 *   2) The Smart Service swallows Exceptions, and caused the node to look like completed successfully even if no sub-process has been started.
 *
 * There are two possible error scenarios, with appropriate error messages from resource bundle:
 * 	 - Looking up of process model ID from UUID failed (process model with the specified UUID does not exist)
 *   - Starting the subprocess failed (most likely because the user who executes this node does not have at least initiator right to the target process model.
 */
@PaletteInfo(paletteCategory = "Standard Nodes", palette = "Activities") 
public class DynamicSubprocessII extends AppianSmartService {
	private static final String PARENT_PROCESS_MODEL_ID = "parentProcessModelId";
	private static final String PARENT_PROCESS_ID = "parentProcessId";

	private static final Logger LOG = Logger.getLogger(DynamicSubprocessII.class);
	
	private final SmartServiceContext smartServiceContext;
	private Long modelId;
	private String modelUUID;
	private String subProcessInitiator;
	
	private Long subProcessId;
	
	/**
	 * Constructor with SmartServiceContext
	 * @param smartServiceContext The context of process where this smart service is being called from.
	 */
	public DynamicSubprocessII(SmartServiceContext smartServiceContext) {
		super();
		this.smartServiceContext = smartServiceContext;
	}

	@Override
	public void run() throws SmartServiceException {
		// For lookup with Process Model UUID to work, the user context has to be an administrator
		ServiceContext _sc = ServiceContextFactory.getServiceContext(smartServiceContext.getUsername());
	      
		ProcessExecutionService _pes = ServiceLocator.getProcessExecutionService(_sc);
		ProcessDesignService _adminPds = ServiceLocator.getProcessDesignService(_sc);
	      
	    // If subProcessInitiator is not specified, use the same user from smart service context
		ProcessDesignService _pds = subProcessInitiator == null 
										? _adminPds
										: ServiceLocator.getProcessDesignService(ServiceContextFactory.getServiceContext(subProcessInitiator));

	      if(modelId == null) {
	        try {
	          modelId = _adminPds.getProcessModelByUuid(modelUUID).getId();
	        } catch (Exception _ex) {
	          LOG.error(_ex);
	          throw createException(_ex, "error.invalidUUID");
	        }
	      }
	      
	      
	      try {
	        ProcessVariable[] _subProcessVariables = _adminPds.getProcessModelParameters(modelId);
	        ProcessVariableInstance[] _parentProcessVariables = _pes.getRecursiveProcessVariables(this.smartServiceContext.getProcessProperties().getId(), true);

	        for(int j=0; j < _subProcessVariables.length; j++) {
	           NamedTypedValue _subPV = _subProcessVariables[j];
	          boolean set = false;
	          for(int i=0; i < _parentProcessVariables.length; i++) {
	            ProcessVariableInstance _parentPV = _parentProcessVariables[i];
	            if (_parentPV.getName().equalsIgnoreCase(_subPV.getName()) && 
	                _parentPV.getTypeRef().getId().equals(_subPV.getTypeRef().getId())) {
	              _subPV.setValue(_parentPV.getRunningValue());
	              set = true;
	              break;
	            }
	          }
	          if (!set) {
	            if (_subPV.getName().equalsIgnoreCase(PARENT_PROCESS_ID) && 
	                _subPV.getTypeRef().getId()==AppianType.INTEGER ) {
	              _subPV.setValue(smartServiceContext.getProcessProperties().getId());
	              //parentProcessIdSet = true;
	            } else if (_subPV.getName().equalsIgnoreCase(PARENT_PROCESS_MODEL_ID) && 
	                _subPV.getTypeRef().getId()==AppianType.INTEGER ) {
	              _subPV.setValue(smartServiceContext.getProcessModelProperties().getId());
	              //parentProcessModelIdSet = true;
	            }
	          }
	        }
	        
	        ProcessStartConfig _processStartConfig = new ProcessStartConfig(_subProcessVariables);
	        this.subProcessId = _pds.initiateProcess(modelId, _processStartConfig);
	        
	      } catch (Exception _ex) {
	        LOG.error(_ex);
	        throw createException(_ex, "error.initiateProcess", modelId, modelUUID);
	      }
	}

	public Long getSubProcessId() {
		return subProcessId;
	}

	public void setModelId(Long modelId) {
		this.modelId = modelId;
	}

	public void setModelUUID(String modelUUID) {
		this.modelUUID = modelUUID;
	}

	public void setSubProcessInitiator(String subProcessInitiator) {
		this.subProcessInitiator = subProcessInitiator;
	}

	private SmartServiceException createException(Throwable t, String key, Object... args) { 
		return new SmartServiceException.Builder(getClass(), t).userMessage(key, args).build(); 
	} 
}
