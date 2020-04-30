package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.API_METHODS_ANNOTATIONS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.API_METHODS_ANNOTATIONS_LISTS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAM;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.cache.impl.CacheInvalidateAllInterceptor;
import io.quarkus.cache.impl.CacheInvalidateInterceptor;
import io.quarkus.cache.impl.CacheResultInterceptor;
import io.quarkus.cache.impl.caffeine.CaffeineCacheBuildRecorder;
import io.quarkus.cache.impl.caffeine.CaffeineCacheInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ManagedExecutorInitializedBuildItem;

class CacheProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CACHE);
    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectCacheName() {
        return new AutoInjectAnnotationBuildItem(CACHE_NAME);
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationsTransformer() {
        return new AnnotationsTransformerBuildItem(new CacheAnnotationsTransformer());
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return Arrays.asList(
                new AdditionalBeanBuildItem(CacheInvalidateAllInterceptor.class),
                new AdditionalBeanBuildItem(CacheInvalidateInterceptor.class),
                new AdditionalBeanBuildItem(CacheResultInterceptor.class));
    }

    @BuildStep
    ValidationErrorBuildItem validateBeanDeployment(ValidationPhaseBuildItem validationPhase) {
        AnnotationStore annotationStore = validationPhase.getContext().get(Key.ANNOTATION_STORE);
        List<Throwable> throwables = new ArrayList<>();
        for (BeanInfo bean : validationPhase.getContext().get(Key.BEANS)) {
            if (bean.isClassBean()) {
                for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                    if (annotationStore.hasAnyAnnotation(method, API_METHODS_ANNOTATIONS)) {
                        CacheMethodValidator.validateAnnotations(annotationStore, bean, method, throwables);
                    }
                }
            }
        }
        return new ValidationErrorBuildItem(throwables.toArray(new Throwable[0]));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void recordCachesBuild(CombinedIndexBuildItem combinedIndex, ExecutorBuildItem executor,
            BeanContainerBuildItem beanContainer, CacheConfig config, CaffeineCacheBuildRecorder caffeineRecorder,
            List<AdditionalCacheNameBuildItem> additionalCacheNames,
            Optional<ManagedExecutorInitializedBuildItem> managedExecutorInitialized) {
        Set<String> cacheNames = getCacheNames(combinedIndex.getIndex());
        for (AdditionalCacheNameBuildItem additionalCacheName : additionalCacheNames) {
            cacheNames.add(additionalCacheName.getName());
        }
        validateCacheNameAnnotations(combinedIndex.getIndex(), cacheNames);
        switch (config.type) {
            case CacheDeploymentConstants.CAFFEINE_CACHE_TYPE:
                Set<CaffeineCacheInfo> cacheInfos = CaffeineCacheInfoBuilder.build(cacheNames, config);
                caffeineRecorder.buildCaches(cacheInfos, managedExecutorInitialized.isPresent(), executor.getExecutorProxy(),
                        beanContainer.getValue());
                break;
            default:
                throw new DeploymentException("Unknown cache type: " + config.type);
        }
    }

    private Set<String> getCacheNames(IndexView index) {
        Set<String> cacheNames = new HashSet<>();
        for (DotName cacheAnnotation : API_METHODS_ANNOTATIONS) {
            for (AnnotationInstance annotation : index.getAnnotations(cacheAnnotation)) {
                if (annotation.target().kind() == METHOD) {
                    cacheNames.add(annotation.value(CACHE_NAME_PARAM).asString());
                }
            }
        }
        for (DotName list : API_METHODS_ANNOTATIONS_LISTS) {
            for (AnnotationInstance annotation : index.getAnnotations(list)) {
                if (annotation.target().kind() == METHOD) {
                    for (AnnotationInstance nestedAnnotation : annotation.value("value").asNestedArray()) {
                        cacheNames.add(nestedAnnotation.value(CACHE_NAME_PARAM).asString());
                    }
                }
            }
        }
        return cacheNames;
    }

    private void validateCacheNameAnnotations(IndexView index, Set<String> cacheNames) {
        for (AnnotationInstance cacheNameAnnotation : index.getAnnotations(CACHE_NAME)) {
            AnnotationTarget target = cacheNameAnnotation.target();
            if (target.kind() == Kind.FIELD || target.kind() == Kind.METHOD_PARAMETER) {
                String declaringClass;
                if (target.kind() == Kind.FIELD) {
                    declaringClass = target.asField().declaringClass().name().toString();
                } else {
                    declaringClass = target.asMethodParameter().method().declaringClass().name().toString();
                }
                String cacheName = cacheNameAnnotation.value().asString();
                if (!cacheNames.contains(cacheName)) {
                    throw new DeploymentException(
                            "A field or method parameter is annotated with @CacheName(\"" + cacheName + "\") in the "
                                    + declaringClass + " class but there is no cache with this name in the application");
                }
            }
        }
    }
}
