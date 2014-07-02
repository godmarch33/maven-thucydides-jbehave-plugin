package ${scenarioStepsClass.packageName};

<#list scenarioStepsClass.imports as classToImport>
import ${classToImport};
</#list>
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.pages.Pages;
import net.thucydides.core.steps.ScenarioSteps;
import org.jbehave.core.annotations.When;

public class ${scenarioStepsClass.classNamePrefix}Steps extends ScenarioSteps {

    private static final long serialVersionUID = 1L;

    <#list scenarioStepsClass.fieldsSteps as innerSteps>
    @Steps
    private ${innerSteps.className} ${innerSteps.fieldName};

    </#list>
    public ${scenarioStepsClass.classNamePrefix}Steps(Pages pages) {
        super(pages);
    }

    <#list scenarioStepsClass.scenarios as method>
    @When("${method.scenarioName}<#assign lastMethodIndex = method.arguments?size><#list method.arguments as argument> $${argument.argumentName}</#list>")
    public void ${method.methodName}(<#assign lastMethodIndex = method.arguments?size><#list method.arguments as argument><#if (0 < argument_index) && (argument_index < lastMethodIndex)>, </#if>${argument.argumentType}${argument.argumentGenericType} ${argument.argumentName}</#list>)<#assign lastExceptionIndex = method.thrownExceptions?size><#if 0 < lastExceptionIndex> throws </#if><#list method.thrownExceptions as exception><#if (0 < exception_index) && (exception_index < lastExceptionIndex)>, </#if>${exception}</#list> {
        <#list method.stepMethods as step>
        ${step.fieldName}.${step.methodName}(<#assign lastIndex = step.methodArguments?size><#list step.methodArguments as argument><#if (0 < argument_index) && (argument_index < lastIndex)>, </#if>${argument.argumentName}</#list>);
        </#list>
    }

    <#if method.arguments?size != 0 >
    @When("${method.scenarioName}")
    public void ${method.methodName}()<#assign lastExceptionIndex = method.thrownExceptions?size><#if 0 < lastExceptionIndex> throws </#if><#list method.thrownExceptions as exception><#if (0 < exception_index) && (exception_index < lastExceptionIndex)>, </#if>${exception}</#list> {
        <#list method.stepMethods as step>
        ${step.fieldName}.${step.methodName}(<#assign lastIndex = step.methodArguments?size><#list step.methodArguments as argument><#if (0 < argument_index) && (argument_index < lastIndex)>, </#if>${argument.argumentDefaultValue}</#list>);
        </#list>
    }

    </#if>
    </#list>
}