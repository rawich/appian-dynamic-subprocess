# Dynamic Subprocess (Async) Smart Service Plug-in for Appian
> Originally created by Ryan Gates (Appian Corp / April 2013) / Updated by [Rawich Poomrin](https://www.linkedin.com/in/rawich) (November 2015)

Appian Smart Service plug-in to dynamically start asynchronous sub-process using process model id or UUID, which can be determined at run-time.

## Version 2.0.1
 - This version addresses an issue of Exception swallowing. In case of error, and no sub-process is started, the previous version swallows Exception and caused the node to look like completed successfully (from Appian Designer portal and Monitor Process).
 - In this new version, if there is any issue, it will be reported both in the log file, and Alert, and node will be paused by exception. 
 - The node will fail to start the intended sub-process if UUID is used to identify the sub-process and the Run-As user is not a system administrator.
 - There are two main error scenarios, with appropriate error messages from resource bundle: 
 - 1) Looking up of process model ID from UUID failed (permission issue or process model with the specified UUID does not exist)
 - 2) Starting the subprocess failed (most likely because the user who executes this node does not have enough security access to the target process model.

## Contribute

Contributions are always welcome!
You'll need to use "Request Permission to Edit a Component" action in Appian Forum to gain access to update this shared component.

## License

[The MIT License](https://github.com/rawich/appian-dynamic-subprocess/blob/master/LICENSE)
