package com.radware.vdirect.ps

import com.radware.alteon.api.AdcConnection
import com.radware.alteon.sdk.AdcResourceId
import com.radware.alteon.sdk.IAdcContainer
import com.radware.alteon.sdk.IAdcInstance
import com.radware.alteon.workflow.impl.WorkflowAdaptor
import com.radware.alteon.workflow.impl.WorkflowState
import com.radware.alteon.workflow.impl.java.Action
import com.radware.alteon.workflow.impl.java.Outputs
import com.radware.alteon.workflow.impl.java.Param
import com.radware.alteon.workflow.impl.java.Device
import com.radware.alteon.workflow.impl.java.UpgradeWorkflow
import com.radware.alteon.workflow.impl.java.Workflow
import com.radware.alteon.workflow.impl.DeviceConnection
import com.radware.vdirect.client.api.DeviceType
import com.radware.vdirect.scripting.PluginVersion
import com.radware.vdirect.server.VDirectServerClient
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired

import javax.ws.rs.BadRequestException

@Workflow(createAction = 'init',
        deleteAction = 'delete')
class CheckDevices {
    @Autowired
    VDirectServerClient vdirect
    @Autowired
    Logger log
    @Autowired
    WorkflowAdaptor workflow


    @Action(visible = false)
    void init() {
        log.info('I was just created..')
    }

    @UpgradeWorkflow
    static WorkflowState upgrade (VDirectServerClient client, PluginVersion version, WorkflowState state) {

        println "Doing ugrade from version ${version}"
        println "State = ${state.state}"
        println "Props = ${state.parameters}"

        state
    }

    @Action(visible = true)
    @Outputs(@Param(name = 'output', type = 'string'))
    String CheckAllAdcDevices()
     {
         List<DeviceResults> results = []
         List<String> adcNames = getStandaloneAdcs()

         def version = vdirect.getWorkflowManager().getWorkflowTemplate("CheckDevices").version
         log.info("version is ${version}")

         adcNames.each { adc ->
             log.debug String.format("Checking Device %s", adc)
             try{
                 DeviceConnection adcConnection = adcNamesToDeviceConnection(adc)
                 if(!adcConnection.isNetworkReachable(5000)){
                     log.error String.format("Device %s Network Error", adc)
                     results.add(new DeviceResults(adc, "Network Error"))
                 }else if (!adcConnection.testConfigProtocol(5000)){
                     log.error String.format("Device %s SNMP Error", adc)
                     results.add(new DeviceResults(adc, "SNMP Error and Perhaps SSH"))
                 }else if (!adcConnection.testCLIProtocol(5000)){
                     log.error String.format("Device %s SSH\\HTTPS Error", adc)
                     results.add(new DeviceResults(adc, "SSH|HTTPS Error"))
                 }
             }catch (Exception e) {
                results.add(new DeviceResults(adc))
             }
         }
         workflow['output'] = results
    }


    @Action(visible = false)
    @Outputs(@Param(name = 'output', type = 'string'))
    String CheckAllContainerDevices()
    {
        List<DeviceResults> results = []
        List<String> adcNames = getContainerAdcs()

        def version = vdirect.getWorkflowManager().getWorkflowTemplate("CheckDevices").version
        log.info("version is ${version}")

        adcNames.each { adc ->
            log.debug String.format("Checking Device %s", adc)
            try{
                AdcConnection adcConnection = adcContainerNamesToDeviceConnection(adc)
                if(!adcConnection.isNetworkReachable(5000)){
                    log.error String.format("Device %s Network Error", adc)
                    results.add(new DeviceResults(adc, "Network Error"))
                }else if (!adcConnection.testConfigProtocol(5000)){
                    log.error String.format("Device %s SNMP Error", adc)
                    results.add(new DeviceResults(adc, "SNMP Error and Perhaps SSH"))
                }else if (!adcConnection.testCLIProtocol(5000)){
                    log.error String.format("Device %s SSH\\HTTPS Error", adc)
                    results.add(new DeviceResults(adc, "SSH|HTTPS Error"))
                }
            }catch (Exception e) {
                results.add(new DeviceResults(adc))
            }
        }
        workflow['output'] = results
    }

    @Action(visible = false)
    void delete() {
        log.info('I am about to be deleted..')
    }

    List<String> getStandaloneAdcs() {
        List<String> names = new ArrayList<>()
        for (IAdcInstance instance : vdirect.adcManager.list()) {
            //instance.connectionDetails.address
            names.add(instance.name)
        }
        return names
    }

    DeviceConnection adcNamesToDeviceConnection(String name) {
        IAdcInstance adc = vdirect.getAdcManager().get(name)
                .orElseThrow({-> new javax.ws.rs.BadRequestException("Could not find Alteon \"" + (name as String) + "\" from name")});
        DeviceConnection connection = vdirect.connect(null, adc).findFirst()
                .orElseThrow({ -> new javax.ws.rs.BadRequestException("Could not find Alteon connection from instance: " +
                        adc.getAdcInfo().toString())})
        return connection
    }

    List<String> getContainerAdcs() {
        List<String> names = new ArrayList<>()
        for (IAdcContainer instance : vdirect.containerManager.getAll()) {
            //instance.connectionDetails.address
            names.add(instance.name)
        }
        return names
    }

    AdcConnection adcContainerNamesToDeviceConnection(String name) {
        IAdcContainer adc = vdirect.containerManager.findByName(name)
        AdcConnection connection = vdirect.containerManager.getConnection(adc)
        return connection
    }

}