package net.thucydides.maven.plugin;

import net.thucydides.jbehave.ThucydidesStepFactory;
import net.thucydides.jbehave.reflection.Extract;
import net.thucydides.maven.plugin.generate.model.*;
import net.thucydides.maven.plugin.utils.DynamicURLClassLoader;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.parsers.StepMatcher;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.StepCandidate;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.thucydides.maven.plugin.utils.NameUtils.*;

public class ScenarioStepsFactory {


    private final ThucydidesStepFactory thucydidesStepFactory;
    private String rootPackage;
    private List<URL> urlsForCustomClasspath;
    private List<StepCandidate> stepCandidates;

    public ScenarioStepsFactory(String rootPackage, List<URL> urlsForCustomClasspath) {
        this.rootPackage = rootPackage;
        this.urlsForCustomClasspath = urlsForCustomClasspath;
        this.thucydidesStepFactory = new ThucydidesStepFactory(new MostUsefulConfiguration(), rootPackage, getClassLoader());
    }

    private ClassLoader getClassLoader() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        DynamicURLClassLoader dynamicURLClassLoader = new DynamicURLClassLoader((URLClassLoader) classLoader);
        for (URL url : urlsForCustomClasspath) {
            dynamicURLClassLoader.addURL(url);
        }
        return dynamicURLClassLoader;
    }

    private List<StepCandidate> findStepCandidates() {
        List<StepCandidate> stepCandidates = new ArrayList<StepCandidate>();
        for (CandidateSteps candidateSteps : thucydidesStepFactory.createCandidateSteps()) {
            stepCandidates.addAll(candidateSteps.listCandidates());
        }
        return stepCandidates;
    }

    public List<StepCandidate> getStepCandidates() {
        if (stepCandidates == null) {
            stepCandidates = findStepCandidates();
        }
        return stepCandidates;
    }

    public ScenarioStepsClassModel createScenarioStepsClassModelFrom(Story story) {
        ScenarioStepsClassModel scenarioStepsClassModel = new ScenarioStepsClassModel();
        scenarioStepsClassModel.setPackageName(rootPackage);
        scenarioStepsClassModel.setClassNamePrefix(getClassNameFrom(story.getName()));
        Set<String> imports = new HashSet<String>();
        Set<FieldsSteps> fieldSteps = new HashSet<FieldsSteps>();
        List<ScenarioMethod> scenarios = new ArrayList<ScenarioMethod>();
        for (Scenario scenario : story.getScenarios()) {
            ScenarioMethod scenarioMethod = new ScenarioMethod();
            scenarioMethod.setScenarioName(scenario.getTitle());
            scenarioMethod.setMethodName(getMethodNameFrom(scenario.getTitle()));
            List<MethodArgument> scenarioMethodArguments = new ArrayList<MethodArgument>();
            List<StepMethod> stepMethods = new ArrayList<StepMethod>();
            String previousNonAndStep = null;
            for (String step : scenario.getSteps()) {
                StepMethod matchedStepMethod = getMatchedStepMethodFor(step, previousNonAndStep, imports, scenarioMethodArguments);
                if (matchedStepMethod.getMethodName() == null) {
                    continue;
                }
                FieldsSteps fieldsSteps = new FieldsSteps();
                fieldsSteps.setClassName(matchedStepMethod.getMethodClass().getSimpleName());
                fieldsSteps.setFieldName(matchedStepMethod.getFieldName());
                fieldSteps.add(fieldsSteps);
                stepMethods.add(matchedStepMethod);
                if (!step.startsWith(Keywords.AND)) {
                    previousNonAndStep = step;
                }
            }
            scenarioMethod.setStepMethods(stepMethods);
            scenarioMethod.setArguments(scenarioMethodArguments);
            scenarios.add(scenarioMethod);
        }
        scenarioStepsClassModel.setImports(imports);
        scenarioStepsClassModel.setFieldsSteps(fieldSteps);
        scenarioStepsClassModel.setScenarios(scenarios);
        return scenarioStepsClassModel;
    }

    public StepMethod getMatchedStepMethodFor(String step, String previousNonAndStep, Set<String> imports, List<MethodArgument> scenarioMethodArguments) {
        StepMethod stepMethod = new StepMethod();
        for (StepCandidate candidate : getStepCandidates()) {
            if (candidate.matches(step, previousNonAndStep)) {
                stepMethod.setMethodName(candidate.getMethod().getName());

                Class<?> stepClass = candidate.getMethod().getDeclaringClass();
                stepMethod.setMethodClass(stepClass);
                imports.add(stepClass.getCanonicalName());
                String fieldClassName = stepClass.getSimpleName();
                String fieldName = replaceFirstCharacterToLowerCase(fieldClassName);
                stepMethod.setFieldName(fieldName);
                //get method arguments
                List<MethodArgument> methodArguments = new ArrayList<MethodArgument>();
                StepCandidate stepCandidate = (StepCandidate) Extract.field("stepCandidate").from(candidate);
                StepMatcher stepMatcher = (StepMatcher) Extract.field("stepMatcher").from(stepCandidate);
                String[] parameterNames = stepMatcher.parameterNames();
                for (int i = 0; i < parameterNames.length; i++) {
                    MethodArgument methodArgument = new MethodArgument();
                    methodArgument.setArgumentName(parameterNames[i]);
                    Class<?> argumentClass = candidate.getMethod().getParameterTypes()[i];
                    methodArgument.setArgumentClass(argumentClass);
                    methodArgument.setArgumentType(argumentClass.getSimpleName());
                    imports.add(argumentClass.getCanonicalName());
                    methodArguments.add(methodArgument);
                    scenarioMethodArguments.add(methodArgument);
                }
                stepMethod.setMethodArguments(methodArguments);
                return stepMethod;
            }
        }
        return stepMethod;
    }
}
