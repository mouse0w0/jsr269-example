package com.github.mouse0w0.jsr269;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class ServiceProviderProcessor extends AbstractProcessor {

    private final Map<String, List<String>> services = new HashMap<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_0; // 支持的源代码版本
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() { // 支持的注解类型
        Set<String> set = new HashSet<>();
        set.add(ServiceProvider.class.getName());
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) { // 判断是否为最终轮
            for (Element element : roundEnv.getElementsAnnotatedWith(ServiceProvider.class)) {
                if (!(element instanceof TypeElement)) {
                    continue;
                }
                addService(findAnnoValue(element, ServiceProvider.class.getName(), "value").getValue().toString(), ((TypeElement) element).getQualifiedName().toString());
            }
        } else {
            saveAll();
        }
        return false; // 如果为true，则接下来的处理器不可处理该注解；如果为false，则接下来的处理器可以处理该处理器处理的注解。
    }

    private AnnotationValue findAnnoValue(Element element, String annoType, String key) {
        for (AnnotationMirror anno : element.getAnnotationMirrors()) {
            if (!ServiceProvider.class.getName().equals(anno.getAnnotationType().toString())) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : anno.getElementValues().entrySet()) {
                if (key.equals(entry.getKey().getSimpleName().toString())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private void addService(String service, String provider) {
        services.computeIfAbsent(service, s -> new ArrayList<>()).add(provider);
    }

    private void saveAll() {
        for (Map.Entry<String, List<String>> entry : services.entrySet()) {
            FileObject fileObject = null;
            try {
                fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + entry.getKey());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                e.printStackTrace();
            }

            if (fileObject == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot create file object for " + entry.getKey());
                return;
            }

            try (Writer writer = fileObject.openWriter()) {
                for (String s : entry.getValue()) {
                    writer.append(s).append("\n");
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
