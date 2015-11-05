package com.appian.plugins.dynamic_subprocess;

//import java.util.Locale;

import org.apache.log4j.Logger;

import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.ServiceContextFactory;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.common.ServiceLocator;
//import com.appiancorp.suiteapi.process.PaletteCategory;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessExecutionService;
import com.appiancorp.suiteapi.process.ProcessStartConfig;
import com.appiancorp.suiteapi.process.ProcessVariable;
import com.appiancorp.suiteapi.process.ProcessVariableInstance;
//import com.appiancorp.suiteapi.process.TypedVariableTypes;
//import com.appiancorp.suiteapi.process.engine.ProcessEngineService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.MessageContainer;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.SmartServiceContext;

import com.appiancorp.suiteapi.process.palette.PaletteInfo; 
import com.appiancorp.suiteapi.type.AppianType;

/**
 * 
 * @author Originally created by Ryan Gates (Appian Corp - April 2013) / Modified by Rawich Poomrin (Persistent Systems - November 2015)
 * @version 2.0.1
 * This version addresses an issue of Exception swallowing:
 *   In case of error, and no sub-process is started, the previous version swallows Exception, 
 *   and caused the node to look like completed successfully (from Appian Designer portal and Monitor Process).
 *   In this new version, if there is any issue, it will be reported both in the log file, and Alert, and node will be paused by exception. 
 *   
 * Known issue:
 *	  - The node will fail to start sub-process if UUID is used to identify the sub-process and the Run-As user is not a system administrator.
 *
 * There are two main error scenarios, with appropriate error messages from resource bundle:
 * 	 - Looking up of process model ID from UUID failed (permission issue or process model with the specified UUID does not exist)
 *   - Starting the subprocess failed (most likely because the user who executes this node does not have enough security access to the target process model.
 */
@PaletteInfo(paletteCategory = "Standard Nodes", palette = "Activities") 
public class DynamicSubprocess extends AppianSmartService {

  private static final Logger LOG = Logger.getLogger(DynamicSubprocess.class);
  private final SmartServiceContext smartServiceCtx;
  private Long modelId;
  private String modelUUID;
  private Long subProcessId;

  @SuppressWarnings("deprecation")
  @Override
  public void run() throws SmartServiceException {
 
      ServiceContext sc = ServiceContextFactory.getServiceContext(smartServiceCtx.getUsername());
      
      ProcessExecutionService pes = ServiceLocator.getProcessExecutionService(sc);
      ProcessDesignService pds = ServiceLocator.getProcessDesignService(sc);

      // If Process Model UUID is provided, instead of ID, then the user context need to be an Administrator
      if (modelId == null) {
        try {
          modelId = pds.getProcessModelByUuid(modelUUID).getId();
        } catch (Exception e) {
          LOG.error(e,e);
          throw createException(e, "error.invalidUUID");
        }
      }
      
      try {
        ProcessVariable[] sub = pds.getProcessModelParameters(modelId);
        ProcessVariableInstance[] parent = pes.getRecursiveProcessVariables(this.smartServiceCtx.getProcessProperties().getId(),true);

        //boolean parentProcessIdSet = false;
        //boolean parentProcessModelIdSet = false;
        for (int j=0;j<sub.length;j++){
          ProcessVariable subone = sub[j];
          boolean set = false;
          for (int i=0;i<parent.length;i++){
            ProcessVariableInstance parentone = parent[i];
            if (parentone.getName().equalsIgnoreCase(subone.getName()) && 
                parentone.getTypeRef().getId().equals(subone.getTypeRef().getId())) {
              subone.setValue(parentone.getRunningValue());
              set = true;
              break;
            }
          }
          if (!set) {
            if (subone.getName().equalsIgnoreCase("parentProcessId") && 
                subone.getTypeRef().getId()==AppianType.INTEGER ) {
              subone.setValue(smartServiceCtx.getProcessProperties().getId());
              //parentProcessIdSet = true;
            } else if (subone.getName().equalsIgnoreCase("parentProcessModelId") && 
                subone.getTypeRef().getId()==AppianType.INTEGER ) {
              subone.setValue(smartServiceCtx.getProcessModelProperties().getId());
              //parentProcessModelIdSet = true;
            }
          }
        }
        
        ProcessStartConfig config = new ProcessStartConfig(sub);
        this.subProcessId = pds.initiateProcess(modelId, config );
        
      } catch (Exception e) {
        LOG.error(e, e);
        throw createException(e, "error.initiateProcess", modelId, modelUUID);
      }
  }

  public DynamicSubprocess(SmartServiceContext smartServiceCtx) {
    super();
    this.smartServiceCtx = smartServiceCtx;
  }

  public void onSave(MessageContainer messages) {
  }

  public void validate(MessageContainer messages) {
  }

  @Input(required = Required.OPTIONAL)
  @Name("ModelId")
  public void setModelId(Long val) {
    this.modelId = val;
  }

  @Input(required = Required.OPTIONAL)
  @Name("ModelUUID")
  public void setModelUUID(String val) {
    this.modelUUID = val;
  }

  @Name("SubProcessId")
  public Long getSubProcessId() {
    return subProcessId;
  }

  private SmartServiceException createException(Throwable t, String key, Object... args) { 
	  return new SmartServiceException.Builder(getClass(), t).userMessage(key, args).build(); 
  } 
}
